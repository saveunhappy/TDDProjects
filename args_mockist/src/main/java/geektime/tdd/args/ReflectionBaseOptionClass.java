package geektime.tdd.args;

import java.util.Arrays;

public class ReflectionBaseOptionClass<T> implements OptionClassLondon<T> {
    private Class<T> optionClass;

    public ReflectionBaseOptionClass(Class<T> optionClass) {
        this.optionClass = optionClass;
    }

    @Override
    public String[] getOptionNames() {
        return Arrays.stream(optionClass.getDeclaredConstructors()[0].getParameters())
                .map(parameter -> parameter.getAnnotation(Option.class).value())
                .toArray(String[]::new);
    }

    @Override
    public Class getOptionType(String name) {
        return null;
    }

    @Override
    public T create(Object[] value) {
        return null;
    }
}
