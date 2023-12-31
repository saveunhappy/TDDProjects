package geektime.tdd.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArgsLondonTest {
    @Test
    public void should_parse_int_option() {
        ValueRetrieverLondon retriever = mock(ValueRetrieverLondon.class);
        OptionClassLondon<IntOption> optionClass = mock(OptionClassLondon.class);
        OptionParserLondon parser = mock(OptionParserLondon.class);
        //这个就是获取参数列表
        when(optionClass.getOptionNames()).thenReturn(new String[]{"p"});
        when(optionClass.getOptionType(eq("p"))).thenReturn(int.class);
        //这个就是注解上的@Option("p")后面那个就是-option.value，然后返回String的。
        when(retriever.getValue(eq("p"), eq(new String[]{"-p", "8080"})))
                .thenReturn(new String[]{"8080"});
        //这个就是Integer.parseInt
        when(parser.parse(eq(int.class), eq(new String[]{"8080"}))).thenReturn(8080);
        //这个就是toArray，需要的就是一个Object，返回一个IntOption
        when(optionClass.create(eq(new Object[]{8080}))).thenReturn(new IntOption(8080));

        ArgsLondon<IntOption> args = new ArgsLondon<>(retriever, parser, optionClass);
        IntOption option = args.parse("-p", "8080");

        assertEquals(8080, option.port);
    }

    record IntOption(@Option("p") int port) {}
}