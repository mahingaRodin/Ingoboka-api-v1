package com.ingoboka_api.v1.ussd.menu;

/**
 * Curated USSD strings (Kinyarwanda / English).
 *
 * <p>Keep ASCII-safe and short. Telcos reject special characters; AT requires responses
 * to start with {@code CON } or {@code END } (no leading whitespace).
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
        return "CON Hitamo ururimi / Choose language:\n"
                + "1. Ikinyarwanda\n"
                + "2. English";
    }

    public static String mainMenu(String language) {
        if (isKinyarwanda(language)) {
            return "CON 1. Serivise z ubwishingizi\n"
                    + "2. Ubwishingizi bwange\n"
                    + "3. Ishyura ubwishingizi\n"
                    + "4. Kwiyandikisha\n"
                    + "5. Saba ubufasha\n"
                    + "6. Hitamo ururimi";
        }
        return "CON 1. Insurance services\n"
                + "2. My policies\n"
                + "3. Pay premium\n"
                + "4. Register\n"
                + "5. Help\n"
                + "6. Choose language";
    }

    public static String help(String language) {
        if (isKinyarwanda(language)) {
            return "END Ubufasha\n"
                    + "Kanda kode ya USSD ongera.\n"
                    + "DEMO/SANDBOX - si ubwishingizi bwemewe.";
        }
        return "END Help\n"
                + "Dial the USSD code again anytime.\n"
                + "DEMO/SANDBOX - not commercial insurance.";
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
            return "CON Hitamo ubwoko:\n1. Umuryango\n2. Ubucuruzi\n0. Subira";
        }
        return "CON Choose type:\n1. Family\n2. Business\n0. Back";
    }

    public static String askFullName(String language) {
        if (isKinyarwanda(language)) {
            return "CON Andika amazina yombi:";
        }
        return "CON Enter full name:";
    }

    public static String askBusinessName(String language) {
        if (isKinyarwanda(language)) {
            return "CON Andika izina ry ubucuruzi:";
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
        return mainMenu(isKinyarwanda(language) ? "rw" : "en");
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
