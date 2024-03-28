package fr.cnrs.liris.jpugetgil.sparqltosql.hibernate;

import fr.cnrs.liris.jpugetgil.sparqltosql.dao.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jpugetgil.
 * Singleton for the hibernate session
 */
public class HibernateSessionSingleton {
    private static HibernateSessionSingleton instance;

    private static final Logger log = LoggerFactory.getLogger(HibernateSessionSingleton.class);

    private final SessionFactory sessionFactory;

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static HibernateSessionSingleton getInstance() {
        if (instance == null) {
            instance = new HibernateSessionSingleton();
        }
        return instance;
    }


    private HibernateSessionSingleton() {
        log.info("Creation of the session...");

        sessionFactory = new Configuration()
                .addAnnotatedClass(ResourceOrLiteral.class)
                .addAnnotatedClass(VersionedQuad.class)
                .addAnnotatedClass(VersionedNamedGraph.class)
                .addAnnotatedClass(Version.class)
                .addAnnotatedClass(Workspace.class)
                .configure()
                .buildSessionFactory();
    }
}

