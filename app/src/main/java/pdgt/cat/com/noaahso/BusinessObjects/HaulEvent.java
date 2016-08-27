package pdgt.cat.com.noaahso.BusinessObjects;

import org.joda.time.DateTime;

import pdgt.cat.com.noaahso.MainActivity;

/**
 * Created by smithm24 on 2/24/2016.
 */
public class HaulEvent {
    public double Latitude = 0.0;
    public double Longitude = 0.0;

    public MainActivity.HaulType type;

    public int l_to_d_num = 0;
    public int d_to_l_num = 0;

    public long ID = -1;

    public DateTime Time;

    public HaulEvent(){

    }
}
