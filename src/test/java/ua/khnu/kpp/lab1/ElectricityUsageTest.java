package ua.khnu.kpp.lab1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ElectricityUsageTest {

    private static final double EPSILON = 1e-9;

    @Test
    void calculatesControlValues() {
        ElectricityUsage usage =
                new ElectricityUsage(12450, 12638, 4.32, 30);

        assertEquals(188.0, usage.consumption(), EPSILON);
        assertEquals(812.16, usage.cost(), EPSILON);
        assertEquals(188.0 / 30.0,
                usage.averageDailyConsumption(), EPSILON);
    }

    @Test
    void calculatesAlternativeValues() {
        ElectricityUsage usage =
                new ElectricityUsage(1000, 1150, 3.5, 25);

        assertEquals(150.0, usage.consumption(), EPSILON);
        assertEquals(525.0, usage.cost(), EPSILON);
        assertEquals(6.0,
                usage.averageDailyConsumption(), EPSILON);
    }

    @Test
    void rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityUsage(-1, 100, 4.32, 30));

        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityUsage(200, 100, 4.32, 30));

        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityUsage(100, 200, 0, 30));

        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityUsage(100, 200, 4.32, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityUsage(Double.NaN, 200, 4.32, 30));

        assertThrows(IllegalArgumentException.class,
                () -> new ElectricityUsage(100, Double.POSITIVE_INFINITY, 4.32, 30));
    }
}