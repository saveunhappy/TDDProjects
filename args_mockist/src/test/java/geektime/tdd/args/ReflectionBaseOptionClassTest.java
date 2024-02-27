package geektime.tdd.args;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReflectionBaseOptionClassTest {
    @Test
    public void should_treat_parameter_with_option_annotation_as_option(){
        OptionClassLondon<IntOption> optionClassLondon = new ReflectionBaseOptionClass(IntOption.class);
        Assertions.assertArrayEquals(new String[]{"p"},optionClassLondon.getOptionNames());
    }
    record IntOption(@Option("p") int port) {}
}
