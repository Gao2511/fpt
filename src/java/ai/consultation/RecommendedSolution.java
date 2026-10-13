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
        public final Long oneTimeDeviceFee;

        public WifiCoverageOption(String id, String equipment, Long monthlyAddonPrice, Long oneTimeDeviceFee) {
            this.id = id;
            this.name = "Mesh WiFi " + id;
            this.equipment = equipment;
            this.monthlyAddonPrice = monthlyAddonPrice;
            this.oneTimeDeviceFee = oneTimeDeviceFee;
        }
    }

    public final ProductCatalog.Product internetPackage;
    public final WifiCoverageOption wifiCoverageOption;
    public final List<String> optionalServices;
    public final long monthlyInternetPrice;
    public final Long monthlyAddonPrice;
    public final Long internetInstallationFee;
    public final Long oneTimeDeviceFee;
    public final Long totalMonthlyPrice;
    public final Long initialCost;
    public final String reason;
    public final String confidence;
    public final List<String> sourceIds;

    public RecommendedSolution(ProductCatalog.Product internetPackage,
                               WifiCoverageOption wifiCoverageOption,
                               List<String> optionalServices,
                               Long internetInstallationFee,
                               String reason,
                               String confidence,
                               List<String> sourceIds) {
        if (internetPackage == null) throw new IllegalArgumentException("Internet package is required");
        this.internetPackage = internetPackage;
        this.wifiCoverageOption = wifiCoverageOption;
        this.optionalServices = Collections.unmodifiableList(new ArrayList<>(optionalServices));
        this.monthlyInternetPrice = internetPackage.price;
        this.monthlyAddonPrice = wifiCoverageOption == null ? null : wifiCoverageOption.monthlyAddonPrice;
        this.internetInstallationFee = internetInstallationFee;
        this.oneTimeDeviceFee = wifiCoverageOption == null ? null : wifiCoverageOption.oneTimeDeviceFee;
        this.totalMonthlyPrice = wifiCoverageOption == null
            ? Long.valueOf(monthlyInternetPrice)
            : safeAdd(monthlyInternetPrice, monthlyAddonPrice);
        this.initialCost = wifiCoverageOption == null
            ? internetInstallationFee
            : safeAdd(internetInstallationFee, oneTimeDeviceFee);
        this.reason = reason;
        this.confidence = confidence;
        this.sourceIds = Collections.unmodifiableList(new ArrayList<>(sourceIds));
    }

    private static Long safeAdd(Long left, Long right) {
        if (left == null || right == null || left < 0 || right < 0) return null;
        if (Long.MAX_VALUE - left < right) return null;
        return left + right;
    }

    public static String money(long value) {
        return String.format(Locale.US, "%,d", value).replace(',', '.') + "đ";
    }
}
