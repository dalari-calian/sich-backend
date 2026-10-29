-- ============================================================================
-- Restrições da agenda que o Hibernate (ddl-auto=update) não cria sozinho.
-- Rode depois que a aplicação subir uma vez e criar as tabelas
-- providerworkinghours, appointments e appointmentitems.
-- ============================================================================

-- Garantia final contra dois agendamentos sobrepostos do mesmo prestador
-- (o AppointmentService já checa antes, com a linha do prestador travada).
-- Período semiaberto [startat, endat): terminar às 10:00 e começar às 10:00 não conflita.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointments ADD CONSTRAINT appointmentsnooverlap
    EXCLUDE USING gist (providerid WITH =, tsrange(startat, endat) WITH &&)
    WHERE (status = 'SCHEDULED');

ALTER TABLE appointments ADD CONSTRAINT appointmentsperiod
    CHECK (endat > startat);

ALTER TABLE appointments ADD CONSTRAINT appointmentscustomer
    CHECK (customerid IS NOT NULL OR customername IS NOT NULL);

ALTER TABLE providerworkinghours ADD CONSTRAINT workinghoursperiod
    CHECK (endtime > starttime);

CREATE INDEX IF NOT EXISTS appointmentsproviderstart ON appointments (providerid, startat);
CREATE INDEX IF NOT EXISTS appointmentscustomerstart ON appointments (customerid, startat);
CREATE INDEX IF NOT EXISTS workinghoursproviderday ON providerworkinghours (providerid, weekday);
