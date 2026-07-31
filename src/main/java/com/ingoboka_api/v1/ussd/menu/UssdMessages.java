package com.ingoboka_api.v1.ussd.menu;

/**
 * Curated USSD strings (Kinyarwanda / English).
 *
 * <p>USSD must stay short and offline-safe — do not call Google Translate per request
 * (latency, cost, and flaky network). Edit this catalog when copy changes.
 */
public final class UssdMessages {

    private UssdMessages() {}

    public static boolean isKinyarwanda(String language) {
        return language == null
                || language.isBlank()
                || "rw".equalsIgnoreCase(language)
                || "kin".equalsIgnoreCase(language);
    }

    public static String languageMenu() {
        return """
                CON Hitamo ururimi
                Choose language:
                1. Ikinyarwanda
                2. English""";
    }

    public static String mainMenu(String language) {
        if (isKinyarwanda(language)) {
            return """
                    CON
                    1. Serivise z'ubwishingizi
                    2. Ubwishingizi bwange
                    3. Ishyura ubwishingizi
                    4. Kwiyandikisha
                    5. Saba ubufasha
                    6. Hitamo ururimi""";
        }
        return """
                CON
                1. Insurance services
                2. My policies
                3. Pay premium
                4. Register
                5. Help
                6. Choose language""";
    }

    public static String help(String language) {
        if (isKinyarwanda(language)) {
            return """
                    END Ubufasha
                    Kanda kode ya USSD ongera.
                    DEMO/SANDBOX — si ubwishingizi bwemewe.
                    Support: agressive.one04@gmail.com""";
        }
        return """
                END Help
                Dial the USSD code again anytime.
                DEMO/SANDBOX — not commercial insurance.
                Support: agressive.one04@gmail.com""";
    }

    public static String invalidOption(String language) {
        if (isKinyarwanda(language)) {
            return "CON Hitamo umubare ukwiye.\n0. Subira inyuma";
        }
        return "CON Invalid option.\n0. Back";
    }

    public static String alreadyRegistered(String language, String reference, String name) {
        if (isKinyarwanda(language)) {
            return "END Mwamaze kwiyandikisha.\nRef: " + reference + "\n" + name;
        }
        return "END Already registered.\nRef: " + reference + "\n" + name;
    }

    public static String registrationTypePrompt(String language) {
        if (isKinyarwanda(language)) {
            return """
                    CON Hitamo ubwoko:
                    1. Umuryango
                    2. Ubucuruzi
                    0. Subira""";
        }
        return """
                CON Choose type:
                1. Family
                2. Business
                0. Back""";
    }

    public static String askFullName(String language) {
        if (isKinyarwanda(language)) {
            return "CON Andika amazina yombi:";
        }
        return "CON Enter full name:";
    }

    public static String askBusinessName(String language) {
        if (isKinyarwanda(language)) {
            return "CON Andika izina ry'ubucuruzi:";
        }
        return "CON Enter business name:";
    }

    public static String askDistrict(String language) {
        if (isKinyarwanda(language)) {
            return "CON Andika akarere:";
        }
        return "CON Enter district:";
    }

    public static String registrationSuccess(String language, String reference, String name, String type) {
        if (isKinyarwanda(language)) {
            return "END Kwiyandikisha byagenze neza!\n"
                    + name
                    + " ("
                    + type
                    + ")\nRef: "
                    + reference
                    + "\nSMS yoherejwe.";
        }
        return "END Registration successful!\n"
                + name
                + " ("
                + type
                + ")\nRef: "
                + reference
                + "\nSMS sent.";
    }

    public static String mustRegister(String language) {
        if (isKinyarwanda(language)) {
            return "END Ntabwo mwiyandikishije.\nHitamo 4 kwiyandikisha.";
        }
        return "END You are not registered.\nChoose 4 to register.";
    }

    public static String noServices(String language) {
        if (isKinyarwanda(language)) {
            return "END Nta serivisi zihari.";
        }
        return "END No insurance services available.";
    }

    public static String servicesHeader(String language) {
        if (isKinyarwanda(language)) {
            return "CON Serivise (DEMO):\n";
        }
        return "CON Services (DEMO):\n";
    }

    public static String plansHeader(String language) {
        if (isKinyarwanda(language)) {
            return "CON Hitamo plan:\n";
        }
        return "CON Choose plan:\n";
    }

    public static String noPlans(String language) {
        if (isKinyarwanda(language)) {
            return "END Nta plans zihari.";
        }
        return "END No plans for this product.";
    }

    public static String backLabel(String language) {
        return isKinyarwanda(language) ? "0. Subira" : "0. Back";
    }

    public static String noPolicies(String language) {
        if (isKinyarwanda(language)) {
            return "END Nta bwishingizi buhari.";
        }
        return "END No policies found.";
    }

    public static String policiesHeader(String language) {
        if (isKinyarwanda(language)) {
            return "END Ubwishingizi bwange:\n";
        }
        return "END My policies:\n";
    }

    public static String noPremiumDue(String language) {
        if (isKinyarwanda(language)) {
            return "END Nta kwishyura gukenewe.";
        }
        return "END No premium due right now.";
    }

    public static String payHeader(String language) {
        if (isKinyarwanda(language)) {
            return "CON Ishyura (SANDBOX):\n";
        }
        return "CON Pay premium (SANDBOX):\n";
    }

    public static String paymentInitiated(String language, String amount, String ref) {
        if (isKinyarwanda(language)) {
            return "END Kwishyura byatangijwe (SANDBOX).\nAmafaranga: "
                    + amount
                    + " RWF\nRef: "
                    + ref;
        }
        return "END Payment initiated (SANDBOX).\nAmount: " + amount + " RWF\nRef: " + ref;
    }

    public static String enrollSuccess(String language, String product, String policyRef) {
        if (isKinyarwanda(language)) {
            return "END Mwiyandikishije kuri: "
                    + product
                    + "\nPolicy: "
                    + (policyRef != null ? policyRef : "pending")
                    + "\nDEMO/SANDBOX";
        }
        return "END Enrolled: "
                + product
                + "\nPolicy: "
                + (policyRef != null ? policyRef : "pending")
                + "\nDEMO/SANDBOX";
    }

    public static String languageChanged(String language) {
        if (isKinyarwanda(language)) {
            return mainMenu("rw");
        }
        return mainMenu("en");
    }

    public static String smsRegistrationBody(
            String language, String name, String type, String district, String phone, String reference) {
        if (isKinyarwanda(language)) {
            return "Ingoboka DEMO: Kwiyandikisha "
                    + name
                    + " ("
                    + type
                    + "), "
                    + district
                    + ". Tel "
                    + phone
                    + ". Ref "
                    + reference
                    + ".";
        }
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
                + ".";
    }
}
