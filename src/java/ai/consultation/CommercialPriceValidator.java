package ai.consultation;

/** Server-side arithmetic and bounds for customer-visible commercial totals. */
public final class CommercialPriceValidator {
    private static final long MAX_VND = 100_000_000L;

    private CommercialPriceValidator() {}

    public static Long valid(Long value) {
        return value != null && value >= 0 && value <= MAX_VND ? value : null;
    }

    public static Long monthlyTotal(long internetPrice, Long meshMonthly) {
        Long internet = valid(internetPrice);
        if (internet == null) return null;
        if (meshMonthly == null) return internet;
        Long mesh = valid(meshMonthly);
        return mesh == null || internet > MAX_VND - mesh ? null : internet + mesh;
    }

    /** A displayed combo amount identifies only this base package plus this add-on. */
    public static boolean matchesMonthlyCombination(long internetPrice, Long meshMonthly, long displayedTotal) {
        Long total = monthlyTotal(internetPrice, meshMonthly);
        return total != null && total == displayedTotal;
    }

    /** A Mesh installation fee is the total solution fee and replaces the standard fee. */
    public static Long installationTotal(Long standardFee, Long meshInstallationTotal) {
        Long mesh = valid(meshInstallationTotal);
        return mesh != null ? mesh : valid(standardFee);
    }

    /** Only a promotion whose applicability is confirmed may change a displayed total. */
    public static Long discountedInstallation(Long installationFee, Long discount, boolean eligibilityConfirmed) {
        Long fee = valid(installationFee);
        if (fee == null || !eligibilityConfirmed) return fee;
        Long reduction = valid(discount);
        if (reduction == null || reduction > fee) return null;
        return fee - reduction;
    }
}
