package net.momo.silo.persistence;

import java.io.IOException;

/** Contract for data persistence backends. */
public interface PersistenceProvider {

    /** Loads all data from storage into registries. */
    void load() throws IOException;

    /** Saves all data from registries to storage. */
    void save() throws IOException;
}
