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

        when(optionClass.getOptionNames()).thenReturn(new String[]{"p"});
        when(optionClass.getOptionType(eq("p"))).thenReturn(int.class);
        when(retriever.getValue(eq("p"), eq(new String[]{"-p", "8080"})))
                .thenReturn(new String[]{"8080"});
        when(parser.parse(eq(int.class), eq(new String[]{"8080"}))).thenReturn(8080);
        when(optionClass.create(eq(new Object[]{8080}))).thenReturn(new IntOption(8080));

        ArgsLondon<IntOption> args = new ArgsLondon<>(retriever, parser, optionClass);
        IntOption option = args.parse("-p", "8080");

        assertEquals(8080, option.port);
    }

    record IntOption(@Option("p") int port) {}
}