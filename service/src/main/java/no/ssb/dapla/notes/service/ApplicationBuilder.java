package no.ssb.dapla.notes.service;

import no.ssb.helidon.application.DefaultHelidonApplicationBuilder;

import static java.util.Optional.ofNullable;

public class ApplicationBuilder extends DefaultHelidonApplicationBuilder {

    @Override
    public Application build() {
        return new Application(ofNullable(config).orElseGet(() -> createDefaultConfig()));
    }
}
