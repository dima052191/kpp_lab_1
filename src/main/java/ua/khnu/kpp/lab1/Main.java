package ua.khnu.kpp.lab1;

import java.util.Locale;
import java.util.Scanner;

public final class Main {

    private static final String ERROR = "ERROR";

    private Main() {
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in)
                .useLocale(Locale.ROOT);

        if (!input.hasNextDouble()) {
            System.out.println(ERROR);
            return;
        }
        double previous = input.nextDouble();

        if (!input.hasNextDouble()) {
            System.out.println(ERROR);
            return;
        }
        double current = input.nextDouble();

        if (!input.hasNextDouble()) {
            System.out.println(ERROR);
            return;
        }
        double tariff = input.nextDouble();

        if (!input.hasNextInt()) {
            System.out.println(ERROR);
            return;
        }
        int days = input.nextInt();

        if (input.hasNext()) {
            System.out.println(ERROR);
            return;
        }

        try {
            ElectricityUsage usage =
                    new ElectricityUsage(previous, current, tariff, days);

            System.out.printf(Locale.ROOT,
                    "RESULT_1=%.2f%n", usage.consumption());

            System.out.printf(Locale.ROOT,
                    "RESULT_2=%.2f%n", usage.cost());

            System.out.printf(Locale.ROOT,
                    "RESULT_3=%.2f%n", usage.averageDailyConsumption());

        } catch (IllegalArgumentException exception) {
            System.out.println(ERROR);
        }
    }
}