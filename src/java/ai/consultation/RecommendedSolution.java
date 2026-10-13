package ai.consultation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Server-owned recommendation result. All monetary values are copied from the
 * structured package catalog or approved CMS business configuration.
 */
public final class RecommendedSolution {
    public static final class WifiCoverageOption {
        public final String id;
        public final String name;
        public final String equipment;
        public final Long monthlyAddonPrice;
        /** Total installation fee for this Mesh solution; it replaces the standard fee. */
        public final Long installationFeeTotal;

        public WifiCoverageOption(String id, String equipment, Long monthlyAddonPrice, Long installationFeeTotal) {
            this.id = id;
            this.name = "Mesh WiFi " + id;
            this.equipment = equipment;
            this.monthlyAddonPrice = monthlyAddonPrice;
            this.installationFeeTotal = CommercialPriceValidator.valid(installationFeeTotal);
        }
    }

    public final ProductCatalog.Product internetPackage;
    public final WifiCoverageOption wifiCoverageOption;
    public final List<String> optionalServices;
    public final long monthlyInternetPrice;
    public final Long monthlyAddonPrice;
    public final Long standardInternetInstallationFee;
    public final Long installationFee;
    public final Long totalMonthlyPrice;
    public final String confirmedPromotion;
    public final Long finalInstallationFee;
    public final String reason;
    public final String confidence;
    public final List<String> sourceIds;
    public final List<String> missingConditions;

    public RecommendedSolution(ProductCatalog.Product internetPackage,
                               WifiCoverageOption wifiCoverageOption,
                               List<String> optionalServices,
                               Long internetInstallationFee,
                               String reason,
                               String confidence,
                               List<String> sourceIds) {
        this(internetPackage, wifiCoverageOption, optionalServices, internetInstallationFee, reason, confidence,
            sourceIds, null, null, null, false);
    }

    public RecommendedSolution(ProductCatalog.Product internetPackage,
                               WifiCoverageOption wifiCoverageOption,
                               List<String> optionalServices,
                               Long internetInstallationFee,
                               String reason,
                               String confidence,
                               List<String> sourceIds,
                               Integer prepaymentMonths,
                               Long installationDiscount,
                               Integer prepay12BonusMonths,
                               boolean promotionConfirmed) {
        if (internetPackage == null) throw new IllegalArgumentException("Internet package is required");
        this.internetPackage = internetPackage;
        this.wifiCoverageOption = wifiCoverageOption;
        this.optionalServices = Collections.unmodifiableList(new ArrayList<>(optionalServices));
        this.monthlyInternetPrice = internetPackage.price;
        this.monthlyAddonPrice = wifiCoverageOption == null ? null : CommercialPriceValidator.valid(wifiCoverageOption.monthlyAddonPrice);
        this.standardInternetInstallationFee = CommercialPriceValidator.valid(internetInstallationFee);
        this.installationFee = CommercialPriceValidator.installationTotal(
            standardInternetInstallationFee, wifiCoverageOption == null ? null : wifiCoverageOption.installationFeeTotal);
        this.totalMonthlyPrice = wifiCoverageOption == null ? CommercialPriceValidator.valid(monthlyInternetPrice)
            : monthlyAddonPrice == null ? null : CommercialPriceValidator.monthlyTotal(monthlyInternetPrice, monthlyAddonPrice);
        boolean eligible = promotionConfirmed && CommercialPriceValidator.valid(installationDiscount) != null
            && (Integer.valueOf(6).equals(prepaymentMonths) || Integer.valueOf(12).equals(prepaymentMonths));
        this.finalInstallationFee = CommercialPriceValidator.discountedInstallation(installationFee, installationDiscount, eligible);
        this.confirmedPromotion = !eligible ? null : Integer.valueOf(12).equals(prepaymentMonths)
            ? "Trả trước 12 tháng: giảm " + money(installationDiscount)
                + " phí lắp" + (prepay12BonusMonths != null && prepay12BonusMonths > 0 ? " và tặng " + prepay12BonusMonths + " tháng dịch vụ" : "")
            : "Trả trước 6 tháng: giảm " + money(installationDiscount) + " phí lắp";
        this.reason = reason;
        this.confidence = confidence;
        this.sourceIds = Collections.unmodifiableList(new ArrayList<>(sourceIds));
        List<String> unknowns = new ArrayList<>();
        unknowns.add("Ngày hết hiệu lực của ưu đãi trả trước chưa được cung cấp");
        this.missingConditions = Collections.unmodifiableList(unknowns);
    }

    public static String money(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', '.') + "đ";
    }
}
