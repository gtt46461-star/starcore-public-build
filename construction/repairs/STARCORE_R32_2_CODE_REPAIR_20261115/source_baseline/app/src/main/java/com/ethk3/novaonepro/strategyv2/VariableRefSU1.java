package com.ethk3.novaonepro.strategyv2;

/** Position and number identifiers cannot be substituted merely because both range from 1 to 10. */
public final class VariableRefSU1 {
    public enum Domain { POSITION, NUMBER }
    public enum Binding { FIXED, BOUND, POSITION_OF_NUMBER }
    public enum Identity { IDENTITY_SENSITIVE, RELABEL_EQUIVARIANT }
    public final Domain domain;
    public final Binding binding;
    public final Identity identity;
    public final String name;
    public final int value, lag;

    public VariableRefSU1(Domain domain, Binding binding, Identity identity, String name, int value, int lag) {
        if (domain == null || binding == null || identity == null || name == null
                || !name.matches("[a-zA-Z][a-zA-Z0-9_]{0,31}") || lag < 0 || lag > 5000
                || binding == Binding.FIXED && (value < 1 || value > 10)
                || binding != Binding.FIXED && value != 0
                || binding == Binding.POSITION_OF_NUMBER && (domain != Domain.POSITION || lag < 1))
            throw new IllegalArgumentException("SU1_VARIABLE_BINDING");
        this.domain = domain;
        this.binding = binding;
        this.identity = identity;
        this.name = name;
        this.value = value;
        this.lag = lag;
    }

    public void require(Domain expected) {
        if (domain != expected) throw new IllegalArgumentException("SU1_POSITION_NUMBER_SUBSTITUTION");
    }

    public String canonical(int boundOrdinal) {
        return domain + "|" + binding + "|" + identity + "|v" + boundOrdinal + "|" + value + "|" + lag;
    }
}
