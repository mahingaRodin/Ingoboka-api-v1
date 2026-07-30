package com.ingoboka_api.v1.ussd.menu;

public final class UssdMessages {

    private UssdMessages() {}

    public static String mainMenu() {
        return """
                CON Ingoboka *477#
                1. Serivise z'ubwishingizi
                2. Ubwishingizi bwange
                3. Ishyura ubwishingizi
                4. Kwiyandikisha
                5. Saba ubufasha""";
    }

    public static String help() {
        return """
                END Ubufasha / Help
                Dial *477# anytime.
                DEMO/SANDBOX only — not commercial insurance.
                Support: agressive.one04@gmail.com""";
    }

    public static String invalidOption() {
        return "CON Hitamo umubare ukwiye / Invalid option.\n0. Subira inyuma";
    }

    public static String alreadyRegistered(String reference, String name) {
        return "END Mwamaze kwiyandikisha.\nAlready registered.\nRef: "
                + reference
                + "\n"
                + name;
    }

    public static String registrationTypePrompt() {
        return """
                CON Hitamo ubwoko / Type:
                1. Umuryango / Family
                2. Ubucuruzi / Business
                0. Subira""";
    }

    public static String askFullName() {
        return "CON Andika amazina yombi\nEnter full name:";
    }

    public static String askBusinessName() {
        return "CON Andika izina ry'ubucuruzi\nEnter business name:";
    }

    public static String askDistrict() {
        return "CON Andika akarere / District:";
    }

    public static String registrationSuccess(String reference, String name, String type) {
        return "END Kwiyandikisha byagenze neza!\nRegistered: "
                + name
                + " ("
                + type
                + ")\nRef: "
                + reference
                + "\nSMS iraza / SMS sent.";
    }

    public static String mustRegister() {
        return "END Ntabwo mwiyandikishije.\nPlease register first (menu 4).";
    }

    public static String noServices() {
        return "END Nta serivisi zihari.\nNo insurance services available.";
    }

    public static String noPolicies() {
        return "END Nta bwishingizi buhari.\nNo policies found.";
    }

    public static String noPremiumDue() {
        return "END Nta kwishyura gukenewe.\nNo premium due right now.";
    }

    public static String paymentInitiated(String amount, String ref) {
        return "END Kwishyura byatangijwe (SANDBOX).\nAmount: "
                + amount
                + " RWF\nRef: "
                + ref;
    }

    public static String enrollSuccess(String product, String policyRef) {
        return "END Mwiyandikishije kuri: "
                + product
                + "\nPolicy: "
                + (policyRef != null ? policyRef : "pending")
                + "\nDEMO/SANDBOX";
    }

    public static String smsRegistrationBody(
            String name, String type, String district, String phone, String reference) {
        return "Ingoboka DEMO: Registered "
                + name
                + " ("
                + type
                + "), "
                + district
                + ". Phone "
                + phone
                + ". Ref "
                + reference
                + ". Dial *477# for services.";
    }
}
