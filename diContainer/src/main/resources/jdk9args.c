 /*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <sys/stat.h>
#include <ctype.h>

#ifdef DEBUG_ARGFILE
  #ifndef NO_JNI
    #define NO_JNI
  #endif
  #define JLI_ReportMessage(...) printf(__VA_ARGS__)
  #define JDK_JAVA_OPTIONS "JDK_JAVA_OPTIONS"
  int IsWhiteSpaceOption(const char* name) { return 1; }
#else
  #include "java.h"
#endif

#include "jli_util.h"
#include "emessages.h"

#define MAX_ARGF_SIZE 0x7fffffffL

static char* clone_substring(const char *begin, size_t len) {
    char *rv = (char *) JLI_MemAlloc(len + 1);
    memcpy(rv, begin, len);
    rv[len] = '\0';
    return rv;
}

enum STATE {
    FIND_NEXT,
    IN_COMMENT,
    IN_QUOTE,
    IN_ESCAPE,
    SKIP_LEAD_WS,
    IN_TOKEN
};

typedef struct {
    enum STATE state;
    const char* cptr;
    const char* eob;
    char quote_char;
    JLI_List parts;
} __ctx_args;

#define NOT_FOUND -1
static int firstAppArgIndex = NOT_FOUND;

static jboolean expectingNoDashArg = JNI_FALSE;
// Initialize to 1, as the first argument is the app name and not preprocessed
static size_t argsCount = 1;
static jboolean stopExpansion = JNI_FALSE;
static jboolean relaunch = JNI_FALSE;

void JLI_InitArgProcessing(jboolean isJava, jboolean disableArgFile) {
    // No expansion for relaunch
    if (argsCount != 1) {
        relaunch = JNI_TRUE;
        stopExpansion = JNI_TRUE;
        argsCount = 1;
    } else {
        stopExpansion = disableArgFile;
    }

    expectingNoDashArg = JNI_FALSE;

    // for tools, this value remains 0 all the time.
    firstAppArgIndex = isJava ? NOT_FOUND : 0;
}

int JLI_GetAppArgIndex() {
    // Will be 0 for tools
    return firstAppArgIndex;
}

static void checkArg(const char *arg) {
    size_t idx = 0;
    argsCount++;

    // All arguments arrive here must be a launcher argument,
    // ie. by now, all argfile expansions must have been performed.
    if (*arg == '-') {
        expectingNoDashArg = JNI_FALSE;
        if (IsWhiteSpaceOption(arg)) {
            // expect an argument
            expectingNoDashArg = JNI_TRUE;

            if (JLI_StrCmp(arg, "-jar") == 0 ||
                JLI_StrCmp(arg, "--module") == 0 ||
                JLI_StrCmp(arg, "-m") == 0) {
                // This is tricky, we do expect NoDashArg
                // But that is considered main class to stop expansion
                expectingNoDashArg = JNI_FALSE;
                // We can not just update the idx here because if -jar @file
                // still need expansion of @file to get the argument for -jar
            }
        } else if (JLI_StrCmp(arg, "--disable-@files") == 0) {
            stopExpansion = JNI_TRUE;
        }
    } else {
        if (!expectingNoDashArg) {
            // this is main class, argsCount is index to next arg
            idx = argsCount;
        }
        expectingNoDashArg = JNI_FALSE;
    }
    // only update on java mode and not yet found main class
    if (firstAppArgIndex == NOT_FOUND && idx != 0) {
        firstAppArgIndex = (int) idx;
    }
}

/*
       [\n\r]   +------------+                        +------------+ [\n\r]
      +---------+ IN_COMMENT +<------+                | IN_ESCAPE  +---------+
      |         +------------+       |                +------------+         |
      |    [#]       ^               |[#]                 ^     |            |
      |   +----------+               |                [\\]|     |[^\n\r]     |
      v   |                          |                    |     v            |
+------------+ [^ \t\n\r\f]  +------------+['"]>      +------------+         |
| FIND_NEXT  +-------------->+ IN_TOKEN   +-----------+ IN_QUOTE   +         |
+------------+               +------------+   <[quote]+------------+         |
  |   ^                          |                       |  ^   ^            |
  |   |               [ \t\n\r\f]|                 [\n\r]|  |   |[^ \t\n\r\f]v
  |   +--------------------------+-----------------------+  |  +--------------+
  |                       ['"]                              |  | SKIP_LEAD_WS |
  +---------------------------------------------------------+  +--------------+
*/
static char* nextToken(__ctx_args *pctx) {
    const char* nextc = pctx->cptr;
    const char* const eob = pctx->eob;
    const char* anchor = nextc;
    char *token;

    for (; nextc < eob; nextc++) {
        register char ch = *nextc;

        // Skip white space characters
        if (pctx->state == FIND_NEXT || pctx->state == SKIP_LEAD_WS) {
            while (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t' || ch == '\f') {
                nextc++;
                if (nextc >= eob) {
                    return NULL;
                }
                ch = *nextc;
            }
            //刚开始，这边就是开始找参数或者是双引号中的参数，
            pctx->state = (pctx->state == FIND_NEXT) ? IN_TOKEN : IN_QUOTE;
            //没移动前anchor是和当前指针指向的下一个字符是一致的
            anchor = nextc;
        // Deal with escape sequences
        } else if (pctx->state == IN_ESCAPE) {
            // concatenation directive
            if (ch == '\n' || ch == '\r') {
                pctx->state = SKIP_LEAD_WS;
            } else {
            // escaped character
                char* escaped = (char*) JLI_MemAlloc(2 * sizeof(char));
                escaped[1] = '\0';
                switch (ch) {
                    case 'n':
                        escaped[0] = '\n';
                        break;
                    case 'r':
                        escaped[0] = '\r';
                        break;
                    case 't':
                        escaped[0] = '\t';
                        break;
                    case 'f':
                        escaped[0] = '\f';
                        break;
                    default:
                        escaped[0] = ch;
                        break;
                }
                JLI_List_add(pctx->parts, escaped);
                pctx->state = IN_QUOTE;
            }
            // anchor to next character
            anchor = nextc + 1;
            continue;
        // ignore comment to EOL
        } else if (pctx->state == IN_COMMENT) {
            while (ch != '\n' && ch != '\r') {
                nextc++;
                if (nextc > eob) {
                    return NULL;
                }
                ch = *nextc;
            }
            pctx->state = FIND_NEXT;
            continue;
        }

        assert(pctx->state != IN_ESCAPE);
        assert(pctx->state != FIND_NEXT);
        assert(pctx->state != SKIP_LEAD_WS);
        assert(pctx->state != IN_COMMENT);

        switch(ch) {
            case ' ':
            case '\t':
            case '\f':
                if (pctx->state == IN_QUOTE) {
                    continue;
                }
                // fall through
            case '\n':
            case '\r':
                if (pctx->parts->size == 0) {
                    token = clone_substring(anchor, nextc - anchor);
                } else {
                    JLI_List_addSubstring(pctx->parts, anchor, nextc - anchor);
                    token = JLI_List_combine(pctx->parts);
                    JLI_List_free(pctx->parts);
                    pctx->parts = JLI_List_new(4);
                }
                pctx->cptr = nextc + 1;
                pctx->state = FIND_NEXT;
                return token;
            case '#':
                if (pctx->state == IN_QUOTE) {
                    continue;
                }
                pctx->state = IN_COMMENT;
                break;
            case '\\':
                if (pctx->state != IN_QUOTE) {
                    continue;
                }
                JLI_List_addSubstring(pctx->parts, anchor, nextc - anchor);
                pctx->state = IN_ESCAPE;

                break;
            case '\'':
            case '"':
                if (pctx->state == IN_QUOTE && pctx->quote_char != ch) {
                    // not matching quote
                    continue;
                }
                // partial before quote
                if (anchor != nextc) {
                    JLI_List_addSubstring(pctx->parts, anchor, nextc - anchor);
                }
                // anchor after quote character
                anchor = nextc + 1;
                if (pctx->state == IN_TOKEN) {
                    pctx->quote_char = ch;
                    pctx->state = IN_QUOTE;
                } else {
                    pctx->state = IN_TOKEN;
                }
                break;
            default:
                break;
        }
    }

    assert(nextc == eob);
    if (anchor != nextc) {
        // not yet return until end of stream, we have part of a token.
        JLI_List_addSubstring(pctx->parts, anchor, nextc - anchor);
    }
    return NULL;
}
//读取参数
static JLI_List readArgFile(FILE *file) {
    //缓存的大小
    char buf[4096];
    JLI_List rv;
    __ctx_args ctx;
    size_t size;
    char *token;

    ctx.state = FIND_NEXT;
    ctx.parts = JLI_List_new(4);

    /* arbitrarily pick 8, seems to be a reasonable number of arguments */
    rv = JLI_List_new(8);

    while (!feof(file)) {
        /* 每次读取4096个字节 */
        size = fread(buf, sizeof(char), sizeof(buf), file);
        if (ferror(file)) {
            JLI_List_free(rv);
            return NULL;
        }

        /* nextc is next character to read from the buffer
         * eob is the end of input
         * token is the copied token value, NULL if no a complete token
         */
        ctx.cptr = buf;
        ctx.eob = buf + size;
        token = nextToken(&ctx);
        while (token != NULL) {
            //检查参数 例如java -jar,token就是token
            checkArg(token);
            //添加到一个list中去
            JLI_List_add(rv, token);
            //继续解析下一个token
            token = nextToken(&ctx);
        }
    }

    // remaining partial token
    if (ctx.state == IN_TOKEN || ctx.state == IN_QUOTE) {
        if (ctx.parts->size != 0) {
            JLI_List_add(rv, JLI_List_combine(ctx.parts));
        }
    }
    JLI_List_free(ctx.parts);

    return rv;
}

/*
 * if the arg represent a file, that is, prefix with a single '@',
 * return a list of arguments from the file.
 * otherwise, return NULL.
 */
static JLI_List expandArgFile(const char *arg) {
    FILE *fptr;
    struct stat st;
    JLI_List rv;

    /* failed to access the file */
    if (stat(arg, &st) != 0) {
        JLI_ReportMessage(CFG_ERROR6, arg);
        exit(1);
    }

    if (st.st_size > MAX_ARGF_SIZE) {
        JLI_ReportMessage(CFG_ERROR10, MAX_ARGF_SIZE);
        exit(1);
    }

    fptr = fopen(arg, "r");
    /* arg file cannot be openned */
    if (fptr == NULL) {
        JLI_ReportMessage(CFG_ERROR6, arg);
        exit(1);
    }

    rv = readArgFile(fptr);
    fclose(fptr);

    /* error occurred reading the file */
    if (rv == NULL) {
        JLI_ReportMessage(DLL_ERROR4, arg);
        exit(1);
    }

    return rv;
}

JLI_List JLI_PreprocessArg(const char *arg)
{
    JLI_List rv;

    if (firstAppArgIndex > 0) {
        // In user application arg, no more work.
        return NULL;
    }

    if (stopExpansion) {
        // still looking for user application arg
        checkArg(arg);
        return NULL;
    }

    if (arg[0] != '@') {
        checkArg(arg);
        return NULL;
    }

    if (arg[1] == '\0') {
        // @ by itself is an argument
        checkArg(arg);
        return NULL;
    }

    arg++;
    if (arg[0] == '@') {
        // escaped @argument
        rv = JLI_List_new(1);
        checkArg(arg);
        JLI_List_add(rv, JLI_StringDup(arg));
    } else {
        rv = expandArgFile(arg);
    }
    return rv;
}

int isTerminalOpt(char *arg) {
    return JLI_StrCmp(arg, "-jar") == 0 ||
           JLI_StrCmp(arg, "-m") == 0 ||
           JLI_StrCmp(arg, "--module") == 0 ||
           JLI_StrCmp(arg, "--dry-run") == 0 ||
           JLI_StrCmp(arg, "-h") == 0 ||
           JLI_StrCmp(arg, "-?") == 0 ||
           JLI_StrCmp(arg, "-help") == 0 ||
           JLI_StrCmp(arg, "--help") == 0 ||
           JLI_StrCmp(arg, "-X") == 0 ||
           JLI_StrCmp(arg, "--help-extra") == 0 ||
           JLI_StrCmp(arg, "-version") == 0 ||
           JLI_StrCmp(arg, "--version") == 0 ||
           JLI_StrCmp(arg, "-fullversion") == 0 ||
           JLI_StrCmp(arg, "--full-version") == 0;
}

jboolean JLI_AddArgsFromEnvVar(JLI_List args, const char *var_name) {
    char *env = getenv(var_name);
    char *p, *arg;
    char quote;
    JLI_List argsInFile;

    if (firstAppArgIndex == 0) {
        // Not 'java', return
        return JNI_FALSE;
    }

    if (relaunch) {
        return JNI_FALSE;
    }

    if (NULL == env) {
        return JNI_FALSE;
    }

    JLI_ReportMessage(ARG_INFO_ENVVAR, var_name, env);

    // This is retained until the process terminates as it is saved as the args
    p = JLI_MemAlloc(JLI_StrLen(env) + 1);
    while (*env != '\0') {
        while (*env != '\0' && isspace(*env)) {
            env++;
        }

        // Trailing space
        if (*env == '\0') {
            break;
        }

        arg = p;
        while (*env != '\0' && !isspace(*env)) {
            if (*env == '"' || *env == '\'') {
                quote = *env++;
                while (*env != quote && *env != '\0') {
                    *p++ = *env++;
                }

                if (*env == '\0') {
                    JLI_ReportMessage(ARG_ERROR8, var_name);
                    exit(1);
                }
                env++;
            } else {
                *p++ = *env++;
            }
        }

        *p++ = '\0';

        argsInFile = JLI_PreprocessArg(arg);

        if (NULL == argsInFile) {
            if (isTerminalOpt(arg)) {
                JLI_ReportMessage(ARG_ERROR9, arg, var_name);
                exit(1);
            }
            JLI_List_add(args, arg);
        } else {
            size_t cnt, idx;
            char *argFile = arg;
            cnt = argsInFile->size;
            for (idx = 0; idx < cnt; idx++) {
                arg = argsInFile->elements[idx];
                if (isTerminalOpt(arg)) {
                    JLI_ReportMessage(ARG_ERROR10, arg, argFile, var_name);
                    exit(1);
                }
                JLI_List_add(args, arg);
            }
            // Shallow free, we reuse the string to avoid copy
            JLI_MemFree(argsInFile->elements);
            JLI_MemFree(argsInFile);
        }
        /*
         * Check if main-class is specified after argument being checked. It
         * must always appear after expansion, as a main-class could be specified
         * indirectly into environment variable via an @argfile, and it must be
         * caught now.
         */
        if (firstAppArgIndex != NOT_FOUND) {
            JLI_ReportMessage(ARG_ERROR11, var_name);
            exit(1);
        }

        assert (*env == '\0' || isspace(*env));
    }

    return JNI_TRUE;
}

#ifdef DEBUG_ARGFILE
/*
 * Stand-alone sanity test, build with following command line
 * $ CC -DDEBUG_ARGFILE -DNO_JNI -g args.c jli_util.c
 */

void fail(char *expected, char *actual, size_t idx) {
    printf("FAILED: Token[%lu] expected to be <%s>, got <%s>\n", idx, expected, actual);
    exit(1);
}

void test_case(char *case_data, char **tokens, size_t cnt_tokens) {
    size_t actual_cnt;
    char *token;
    __ctx_args ctx;

    actual_cnt = 0;

    ctx.state = FIND_NEXT;
    ctx.parts = JLI_List_new(4);
    ctx.cptr = case_data;
    ctx.eob = case_data + strlen(case_data);

    printf("Test case: <%s>, expected %lu tokens.\n", case_data, cnt_tokens);

    for (token = nextToken(&ctx); token != NULL; token = nextToken(&ctx)) {
        // should not have more tokens than expected
        if (actual_cnt >= cnt_tokens) {
            printf("FAILED: Extra token detected: <%s>\n", token);
            exit(2);
        }
        if (JLI_StrCmp(token, tokens[actual_cnt]) != 0) {
            fail(tokens[actual_cnt], token, actual_cnt);
        }
        actual_cnt++;
    }

    char* last = NULL;
    if (ctx.parts->size != 0) {
        last = JLI_List_combine(ctx.parts);
    }
    JLI_List_free(ctx.parts);

    if (actual_cnt >= cnt_tokens) {
        // same number of tokens, should have nothing left to parse
        if (last != NULL) {
            if (*last != '#') {
                printf("Leftover detected: %s", last);
                exit(2);
            }
        }
    } else {
        if (JLI_StrCmp(last, tokens[actual_cnt]) != 0) {
            fail(tokens[actual_cnt], last, actual_cnt);
        }
        actual_cnt++;
    }
    if (actual_cnt != cnt_tokens) {
        printf("FAILED: Number of tokens not match, expected %lu, got %lu\n",
            cnt_tokens, actual_cnt);
        exit(3);
    }

    printf("PASS\n");
}

#define DO_CASE(name) \
    test_case(name[0], name + 1, sizeof(name)/sizeof(char*) - 1)

int main(int argc, char** argv) {
    size_t i, j;

    char* case1[] = { "-version -cp \"c:\\\\java libs\\\\one.jar\" \n",
        "-version", "-cp", "c:\\java libs\\one.jar" };
    DO_CASE(case1);

    // note the open quote at the end
    char* case2[] = { "com.foo.Panda \"Furious 5\"\fand\t'Shi Fu' \"escape\tprison",
        "com.foo.Panda", "Furious 5", "and", "Shi Fu", "escape\tprison"};
    DO_CASE(case2);

    char* escaped_chars[] = { "escaped chars testing \"\\a\\b\\c\\f\\n\\r\\t\\v\\9\\6\\23\\82\\28\\377\\477\\278\\287\"",
        "escaped", "chars", "testing", "abc\f\n\r\tv96238228377477278287"};
    DO_CASE(escaped_chars);

    char* mixed_quote[]  = { "\"mix 'single quote' in double\" 'mix \"double quote\" in single' partial\"quote me\"this",
        "mix 'single quote' in double", "mix \"double quote\" in single", "partialquote methis"};
    DO_CASE(mixed_quote);

    char* comments[]  = { "line one #comment\n'line #2' #rest are comment\r\n#comment on line 3\nline 4 #comment to eof",
        "line", "one", "line #2", "line", "4"};
    DO_CASE(comments);

    char* open_quote[] = { "This is an \"open quote \n    across line\n\t, note for WS.",
        "This", "is", "an", "open quote ", "across", "line", ",", "note", "for", "WS." };
    DO_CASE(open_quote);

    char* escape_in_open_quote[] = { "Try \"this \\\\\\\\ escape\\n double quote \\\" in open quote",
        "Try", "this \\\\ escape\n double quote \" in open quote" };
    DO_CASE(escape_in_open_quote);

    char* quote[] = { "'-Dmy.quote.single'='Property in single quote. Here a double quote\" Add some slashes \\\\/'",
        "-Dmy.quote.single=Property in single quote. Here a double quote\" Add some slashes \\/" };
    DO_CASE(quote);

    char* multi[] = { "\"Open quote to \n  new \"line \\\n\r   third\\\n\r\\\tand\ffourth\"",
        "Open quote to ", "new", "line third\tand\ffourth" };
    DO_CASE(multi);

    char* escape_quote[] = { "c:\\\"partial quote\"\\lib",
        "c:\\partial quote\\lib" };
    DO_CASE(escape_quote);

    if (argc > 1) {
        for (i = 0; i < argc; i++) {
            JLI_List tokens = JLI_PreprocessArg(argv[i]);
            if (NULL != tokens) {
                for (j = 0; j < tokens->size; j++) {
                    printf("Token[%lu]: <%s>\n", (unsigned long) j, tokens->elements[j]);
                }
            }
        }
    }
}

#endif // DEBUG_ARGFILE
