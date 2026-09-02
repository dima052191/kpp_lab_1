package ua.khnu.kpp.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class Lab1PublicTest {
    private static final String PACKAGE_NAME = "ua.khnu.kpp.lab1.";
    private static final double EPSILON = 1e-9;

    @Test
    void submissionMetadataSelectsKnownVariant() throws IOException {
        VariantSpec spec = selectedVariant();

        assertTrue(spec.number() >= 1 && spec.number() <= 12,
                "Variant must be in the range 1..12");
    }

    @Test
    void subjectClassProvidesRequiredContractAndControlValues()
            throws Exception {
        VariantSpec spec = selectedVariant();
        Object subject = createSubject(spec, spec.controlArguments());

        assertResults(spec, subject, spec.controlExpected());
    }

    @Test
    void subjectClassCalculatesIndependentAlternativeValues()
            throws Exception {
        VariantSpec spec = selectedVariant();
        Object subject = createSubject(spec, spec.alternativeArguments());

        assertResults(spec, subject, spec.alternativeExpected());
    }

    @Test
    void constructorRejectsInvalidDomainValues() throws Exception {
        VariantSpec spec = selectedVariant();
        Constructor<?> constructor = requiredConstructor(spec);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> constructor.newInstance(spec.invalidArguments()),
                "The constructor must reject the invalid test case");
        assertInstanceOf(IllegalArgumentException.class, exception.getCause(),
                "The constructor must throw IllegalArgumentException");
    }

    @Test
    void constructorRejectsNonFiniteFloatingPointValues() throws Exception {
        VariantSpec spec = selectedVariant();
        Constructor<?> constructor = requiredConstructor(spec);
        Object[] arguments = spec.controlArguments().clone();
        int doubleIndex = firstDoubleParameter(spec.constructorTypes());
        arguments[doubleIndex] = Double.NaN;

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> constructor.newInstance(arguments),
                "The constructor must reject NaN");
        assertInstanceOf(IllegalArgumentException.class, exception.getCause(),
                "The constructor must throw IllegalArgumentException for NaN");
    }

    @Test
    void mainPrintsTheControlResultUsingTheRequiredProtocol() throws Throwable {
        VariantSpec spec = selectedVariant();
        String output = runMain(asInput(spec.controlArguments()));

        assertEquals(expectedOutput(spec.controlExpected()), output,
                "Main must print only RESULT_N lines in the specified order");
    }

    @Test
    void mainRejectsNonNumericInputWithoutAStackTrace() throws Throwable {
        selectedVariant();
        assertEquals("ERROR", runMain("abc" + System.lineSeparator()),
                "Invalid input must produce exactly one ERROR line");
    }

    private static VariantSpec selectedVariant() throws IOException {
        Path metadata = Path.of("submission.properties");
        assertTrue(Files.isRegularFile(metadata),
                "submission.properties is missing from the project root");

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(metadata, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        assertEquals("1", properties.getProperty("lab"),
                "submission.properties must contain lab=1");

        String rawVariant = properties.getProperty("variant", "").trim();
        int variant;
        try {
            variant = Integer.parseInt(rawVariant);
        } catch (NumberFormatException exception) {
            return fail("Replace variant=NN with a number from 1 to 12");
        }
        return specification(variant);
    }

    private static Object createSubject(VariantSpec spec, Object[] arguments)
            throws ReflectiveOperationException {
        Class<?> subjectClass;
        try {
            subjectClass = Class.forName(PACKAGE_NAME + spec.className());
        } catch (ClassNotFoundException exception) {
            return fail("Create public class " + spec.className()
                    + " in package ua.khnu.kpp.lab1");
        }
        assertTrue(Modifier.isPublic(subjectClass.getModifiers()),
                spec.className() + " must be public");

        try {
            return requiredConstructor(spec).newInstance(arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            return fail("The constructor rejected valid control data: " + cause);
        }
    }

    private static Constructor<?> requiredConstructor(VariantSpec spec)
            throws ReflectiveOperationException {
        Class<?> subjectClass;
        try {
            subjectClass = Class.forName(PACKAGE_NAME + spec.className());
        } catch (ClassNotFoundException exception) {
            return fail("Create public class " + spec.className()
                    + " in package ua.khnu.kpp.lab1");
        }
        try {
            return subjectClass.getConstructor(spec.constructorTypes());
        } catch (NoSuchMethodException exception) {
            return fail("Required public constructor is missing in "
                    + spec.className() + ": "
                    + Arrays.toString(spec.constructorTypes()));
        }
    }

    private static void assertResults(
            VariantSpec spec, Object subject, double[] expected)
            throws ReflectiveOperationException {
        Class<?> subjectClass = subject.getClass();
        for (int index = 0; index < spec.methodNames().length; index++) {
            String methodName = spec.methodNames()[index];
            Method method;
            try {
                method = subjectClass.getMethod(methodName);
            } catch (NoSuchMethodException exception) {
                fail("Required public method is missing: " + methodName + "()");
                return;
            }
            assertEquals(double.class, method.getReturnType(),
                    methodName + "() must return double");
            Object actual = method.invoke(subject);
            assertEquals(expected[index], ((Number) actual).doubleValue(), EPSILON,
                    "Unexpected result from " + methodName + "()");
        }
    }

    private static int firstDoubleParameter(Class<?>[] types) {
        for (int index = 0; index < types.length; index++) {
            if (types[index] == double.class) {
                return index;
            }
        }
        return fail("Every Lab 1 variant must have a double parameter");
    }

    private static String asInput(Object[] arguments) {
        return String.join(" ", Arrays.stream(arguments)
                .map(String::valueOf)
                .toList()) + System.lineSeparator();
    }

    private static String expectedOutput(double[] values) {
        return String.join("\n",
                IntStream.range(0, values.length)
                        .mapToObj(index -> String.format(Locale.ROOT,
                                "RESULT_%d=%.2f", index + 1, values[index]))
                        .toList());
    }

    private static String runMain(String input) throws Throwable {
        var originalInput = System.in;
        var originalOutput = System.out;
        var output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(
                    input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            Main.main(new String[0]);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }
        return output.toString(StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .stripTrailing();
    }

    private static VariantSpec specification(int variant) {
        return switch (variant) {
            case 1 -> new VariantSpec(1, "Room",
                    types(double.class, double.class, double.class),
                    names("floorArea", "volume", "wallArea"),
                    values(5.0, 4.0, 2.8), expected(20.0, 56.0, 50.4),
                    values(2.0, 3.0, 4.0), expected(6.0, 24.0, 40.0),
                    values(0.0, 4.0, 2.8));
            case 2 -> new VariantSpec(2, "Trip",
                    types(double.class, double.class, double.class),
                    names("fuelConsumptionPer100Km", "totalFuelCost",
                            "costPer100Km"),
                    values(420.0, 31.5, 59.9),
                    expected(7.5, 1886.85, 449.25),
                    values(100.0, 5.0, 10.0), expected(5.0, 50.0, 50.0),
                    values(0.0, 31.5, 59.9));
            case 3 -> new VariantSpec(3, "Deposit",
                    types(double.class, double.class, int.class),
                    names("interest", "finalAmount", "averageMonthlyIncome"),
                    values(10000.0, 8.5, 9),
                    expected(637.5, 10637.5, 70.83333333333333),
                    values(1200.0, 12.0, 6), expected(72.0, 1272.0, 12.0),
                    values(10000.0, 8.5, 0));
            case 4 -> new VariantSpec(4, "Cylinder",
                    types(double.class, double.class),
                    names("volume", "lateralArea", "totalArea"),
                    values(3.0, 5.0),
                    expected(45.0 * Math.PI, 30.0 * Math.PI, 48.0 * Math.PI),
                    values(2.0, 4.0),
                    expected(16.0 * Math.PI, 16.0 * Math.PI, 24.0 * Math.PI),
                    values(0.0, 5.0));
            case 5 -> new VariantSpec(5, "Parcel",
                    types(double.class, double.class, double.class, double.class),
                    names("transportCost", "insuranceCost", "totalCost"),
                    values(12.5, 320.0, 4.8, 1.5),
                    expected(192.0, 2.88, 194.88),
                    values(10.0, 100.0, 2.0, 10.0),
                    expected(20.0, 2.0, 22.0),
                    values(0.0, 320.0, 4.8, 1.5));
            case 6 -> new VariantSpec(6, "ElectricityUsage",
                    types(double.class, double.class, double.class, int.class),
                    names("consumption", "cost", "averageDailyConsumption"),
                    values(12450.0, 12638.0, 4.32, 30),
                    expected(188.0, 812.16, 6.266666666666667),
                    values(100.0, 130.0, 2.0, 10), expected(30.0, 60.0, 3.0),
                    values(200.0, 100.0, 2.0, 10));
            case 7 -> new VariantSpec(7, "HealthMetrics",
                    types(double.class, double.class, double.class),
                    names("bodyMassIndex", "bodySurfaceArea", "targetDifference"),
                    values(78.0, 1.82, 72.0),
                    expected(78.0 / (1.82 * 1.82),
                            Math.sqrt(100.0 * 1.82 * 78.0 / 3600.0), 6.0),
                    values(80.0, 2.0, 75.0),
                    expected(20.0, Math.sqrt(100.0 * 2.0 * 80.0 / 3600.0), 5.0),
                    values(78.0, 0.0, 72.0));
            case 8 -> new VariantSpec(8, "Order",
                    types(double.class, int.class, double.class),
                    names("subtotal", "discountAmount", "amountDue"),
                    values(249.9, 3, 10.0), expected(749.7, 74.97, 674.73),
                    values(100.0, 2, 25.0), expected(200.0, 50.0, 150.0),
                    values(249.9, 0, 10.0));
            case 9 -> new VariantSpec(9, "Route",
                    types(double.class, double.class, double.class),
                    names("travelTimeHours", "totalTimeHours",
                            "actualAverageSpeed"),
                    values(360.0, 80.0, 45.0), expected(4.5, 5.25, 360.0 / 5.25),
                    values(100.0, 50.0, 60.0), expected(2.0, 3.0, 100.0 / 3.0),
                    values(360.0, 0.0, 45.0));
            case 10 -> new VariantSpec(10, "CourseResult",
                    types(double.class, double.class, double.class),
                    names("labContribution", "practicalContribution",
                            "examContribution", "finalScore", "marginAbovePass"),
                    values(82.0, 90.0, 76.0),
                    expected(28.7, 22.5, 30.4, 81.6, 21.6),
                    values(100.0, 100.0, 100.0),
                    expected(35.0, 25.0, 40.0, 100.0, 40.0),
                    values(-1.0, 90.0, 76.0));
            case 11 -> new VariantSpec(11, "Recipe",
                    types(int.class, int.class, double.class, double.class),
                    names("scaleFactor", "scaledFlourGrams",
                            "scaledMilkMilliliters"),
                    values(4, 10, 300.0, 500.0), expected(2.5, 750.0, 1250.0),
                    values(2, 6, 100.0, 200.0), expected(3.0, 300.0, 600.0),
                    values(0, 10, 300.0, 500.0));
            case 12 -> new VariantSpec(12, "CurrencyPurchase",
                    types(double.class, double.class, double.class),
                    names("baseCost", "commissionAmount", "totalCost"),
                    values(250.0, 45.2, 1.2), expected(11300.0, 135.6, 11435.6),
                    values(100.0, 40.0, 2.0), expected(4000.0, 80.0, 4080.0),
                    values(0.0, 45.2, 1.2));
            default -> fail("Variant must be a number from 1 to 12");
        };
    }

    private static Class<?>[] types(Class<?>... values) {
        return values;
    }

    private static String[] names(String... values) {
        return values;
    }

    private static Object[] values(Object... values) {
        return values;
    }

    private static double[] expected(double... values) {
        return values;
    }

    private record VariantSpec(
            int number,
            String className,
            Class<?>[] constructorTypes,
            String[] methodNames,
            Object[] controlArguments,
            double[] controlExpected,
            Object[] alternativeArguments,
            double[] alternativeExpected,
            Object[] invalidArguments) {
    }
}
