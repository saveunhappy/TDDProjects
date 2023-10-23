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


        ArgsLondon<IntOption> args = new ArgsLondon<>(retriever, parser, optionClass);
        IntOption option = args.parse("-p", "8080");

        assertEquals(8080, option.port);
    }

    record IntOption(@Option("p") int port) {}
}