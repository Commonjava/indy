package org.commonjava.indy.schedule;

public class ScheduleDBUtil
{

    public static final String TABLE_SCHEDULE = "schedule";
    public static final String TABLE_EXPIRATION = "expiration";

    public static String getSchemaCreateTableSchedule( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE_SCHEDULE + " ("
                        + "jobtype varchar,"
                        + "jobname varchar,"
                        + "scheduleuid uuid,"
                        + "storekey varchar,"
                        + "payload varchar,"
                        + "scheduletime timestamp,"
                        + "lifespan bigint,"
                        + "expired boolean,"
                        + "PRIMARY KEY (storekey, jobname)"
                        + ");";
    }

    public static String getSchemaCreateTableExpiration( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE_EXPIRATION + " ("
                        + "expirationpid bigint,"
                        + "scheduleuid uuid,"
                        + "expirationtime timestamp,"
                        + "storekey varchar,"
                        + "jobname varchar,"
                        + "PRIMARY KEY (expirationpid, storekey, jobname)"
                        + ");";
    }

    public static String getSchemaCreateTypeIndex4Schedule( String keyspace )
    {
        return "CREATE INDEX IF NOT EXISTS type_idx on " + keyspace + "." + TABLE_SCHEDULE + " (jobtype)";
    }

}
