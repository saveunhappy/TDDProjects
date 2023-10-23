package geektime.tdd.args;

import java.util.Arrays;

public class ArgsLondon<T> {

    private ValueRetrieverLondon retriever;
    private OptionParserLondon parser;
    private OptionClassLondon<T> optionClass;

    public ArgsLondon(ValueRetrieverLondon retriever, OptionParserLondon parser, OptionClassLondon<T> optionClass) {
        this.retriever = retriever;
        this.parser = parser;
        this.optionClass = optionClass;
    }

    public T parse(String... args) {

        return optionClass.create(Arrays.stream(optionClass.getOptionNames())
                .map(name -> parser.parse(
                        optionClass.getOptionType(name),
                        retriever.getValue(name, args))).toArray(Object[]::new));
    }

}