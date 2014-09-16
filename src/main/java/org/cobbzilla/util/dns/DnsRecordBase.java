package org.cobbzilla.util.dns;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.string.StringUtil.empty;

@Accessors(chain=true) @ToString @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class DnsRecordBase {

    @Getter @Setter private String fqdn;
    public boolean hasFqdn() { return !empty(fqdn); }

    @Getter @Setter private DnsType type;
    public boolean hasType () { return type != null; }

    @Getter @Setter private String value;
    public boolean hasValue () { return !empty(value); }

    @JsonIgnore
    public DnsRecordMatch getMatcher() {
        return (DnsRecordMatch) new DnsRecordMatch().setFqdn(fqdn).setType(type).setValue(value);
    }
}