package backend.academy;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;


@State(Scope.Thread)
public class ExecutionPlan {
    private Student student;
    private Method reflectMethod;
    private Function<Student, String> lambdaMetafactory;
    private MethodHandle methodHandle;
    private static final String FIELDNAME = "name";
    private static final int MEASUREMENTTIME = 120;

    @SuppressWarnings("uncommentedmain")
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(ExecutionPlan.class.getSimpleName())
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .forks(1)
                .warmupForks(1)
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(MEASUREMENTTIME))
                .measurementIterations(1)
                .measurementTime(TimeValue.seconds(MEASUREMENTTIME))
                .build();
        new Runner(options).run();

    }

    @Setup
    public void setup()
            throws Throwable {
        student = new Student("Nikita", "Eliseev");
        reflectMethod = student.getClass().getMethod(FIELDNAME);
        MethodHandles.Lookup publicLookup = MethodHandles.lookup();
        methodHandle = publicLookup.findGetter(Student.class, FIELDNAME, String.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle getterHandle = lookup.findVirtual(Student.class, FIELDNAME, MethodType.methodType(String.class));
        CallSite callSite = LambdaMetafactory.metafactory(
                lookup,
                "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                getterHandle,
                MethodType.methodType(String.class, Student.class));

        lambdaMetafactory = (Function<Student, String>) callSite.getTarget().invoke();
    }

    @Benchmark
    public void directAccess(Blackhole blackhole) {
        String name = student.name();
        blackhole.consume(name);
    }

    @Benchmark
    public void reflectiveAccess(Blackhole blackhole)
            throws  InvocationTargetException, IllegalAccessException {
        String name = (String) reflectMethod.invoke(student);
        blackhole.consume(name);
    }

    @Benchmark
    public void methodHandle(Blackhole blackhole)
            throws Throwable {
        String name = (String) methodHandle.invoke(student);
        blackhole.consume(name);

    }

    @Benchmark
    public void lambda(Blackhole blackhole) {

        String name = lambdaMetafactory.apply(student);
        blackhole.consume(name);
    }

    record Student(String name, String surname) {
    }
}
