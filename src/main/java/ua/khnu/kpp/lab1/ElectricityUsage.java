package ua.khnu.kpp.lab1;

public final class ElectricityUsage {

    private final double previous;
    private final double current;
    private final double tariff;
    private final int days;

    public ElectricityUsage(double previous, double current, double tariff, int days) {
        if (!Double.isFinite(previous) || previous < 0
                || !Double.isFinite(current) || current < previous
                || !Double.isFinite(tariff) || tariff <= 0
                || days <= 0) {
            throw new IllegalArgumentException("Invalid electricity usage parameters");
        }

        this.previous = previous;
        this.current = current;
        this.tariff = tariff;
        this.days = days;
    }

    public double consumption() {
        return current - previous;
    }

    public double cost() {
        return consumption() * tariff;
    }

    public double averageDailyConsumption() {
        return consumption() / days;
    }
}