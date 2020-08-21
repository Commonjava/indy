package org.commonjava.indy.schedule.datastax;

public class ScheduleDBUtil
{

    public static final String TABLE_SCHEDULE = "schedule";

    public static String getSchemaCreateTableSchedule( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE_SCHEDULE + " ("
                        + "jobtype varchar,"
                        + "jobname varchar,"
                        + "storekey varchar,"
                        + "scheduletime timestamp,"
                        + "lifespan bigint,"
                        + "expiration varchar,"
                        + "PRIMARY KEY (storekey, jobname)"
                        + ");";
    }

    public static String getSchemaCreateTypeIndex4Schedule( String keyspace )
    {
        return "CREATE INDEX IF NOT EXISTS type_idx on " + keyspace + "." + TABLE_SCHEDULE + " (jobtype)";
    }

}
