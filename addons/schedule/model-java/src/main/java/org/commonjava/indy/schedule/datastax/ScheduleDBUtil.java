package org.commonjava.indy.schedule.datastax;

public class ScheduleDBUtil
{

    public static String getSchemaCreateTableSchedule( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + ".schedule ("
                        + "jobtype varchar,"
                        + "jobname varchar,"
                        + "storekey varchar,"
                        + "scheduletime timestamp,"
                        + "lifescan bigint,"
                        + "expiration varchar,"
                        + "PRIMARY KEY (storekey, jobname)"
                        + ");";
    }

}
