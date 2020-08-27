package org.commonjava.indy.schedule.datastax;

public class ScheduleDBUtil
{

    public static final String TABLE_SCHEDULE = "schedule";

    public static String getSchemaCreateKeyspace( String keyspace )
    {
        return "CREATE KEYSPACE IF NOT EXISTS " + keyspace
                        + " WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1};";
    }

    public static String getSchemaCreateTableSchedule( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE_SCHEDULE + " ("
                        + "jobtype varchar,"
                        + "jobname varchar,"
                        + "storekey varchar,"
                        + "scheduletime timestamp,"
                        + "lifespan bigint,"
                        + "expired boolean,"
                        + "ttl bigint,"
                        + "PRIMARY KEY (storekey, jobname)"
                        + ");";
    }

    public static String getSchemaCreateTypeIndex4Schedule( String keyspace )
    {
        return "CREATE INDEX IF NOT EXISTS type_idx on " + keyspace + "." + TABLE_SCHEDULE + " (jobtype)";
    }

}
