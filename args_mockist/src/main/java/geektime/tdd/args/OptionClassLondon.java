package geektime.tdd.args;

public interface OptionClassLondon<T> {
    String[] getOptionNames();

    Class getOptionType(String name);

    T create(Object[] value);
}