// Keep CMS values out of inline CSS so JSP-aware and CSS editors can parse the view.
(function () {
    document.querySelectorAll('[data-cms-order]').forEach(function (element) {
        var value = element.getAttribute('data-cms-order');
        if (!/^-?\d+$/.test(value)) return;
        var order = Number(value);
        if (!Number.isSafeInteger(order) || order < -2147483648 || order > 2147483647) return;
        element.style.order = String(order);
    });
})();
