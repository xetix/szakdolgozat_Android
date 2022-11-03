package hu.unideb.ik.kovacsgergo.szakdolgozat;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Program implements Comparable<Program> {
    private String mDay;
    private String mHour;
    private Double mSetpoint;

    public Program(String day, String hour, Double setpoint){
        mDay = day;
        mHour = hour;
        mSetpoint = setpoint;
    }

    public static HashMap<String,String> days(){
        HashMap<String,String> days = new HashMap<String,String>();
        days.put("Monday","Hétfő");
        days.put("Tuesday","Kedd");
        days.put("Wednesday","Szerda");
        days.put("Thursday","Csütörtök");
        days.put("Friday","Péntek");
        days.put("Saturday","Szombat");
        days.put("Sunday","Vasárnap");
        return days;
    }


    public String getDay(){
        return mDay;
    }

    public Integer getDayPlaceOfOlder(){
        Integer rtn = null;
        switch(mDay) {
            case "Monday":
                rtn = 1;
                break;
            case "Tuesday":
                rtn = 2;
                break;
            case "Wednesday":
                rtn = 3;
                break;
            case "Thursday":
                rtn = 4;
                break;
            case "Friday":
                rtn = 5;
                break;
            case "Saturday":
                rtn = 6;
                break;
            case "Sunday":
                rtn = 7;
                break;
        }
        return rtn;
    }

    public String getDayHun(){
        return days().get(mDay);
    }
    
    public static String translateDay(String day){
        for (Map.Entry<String, String> set : days().entrySet()) {
            if( day.equals(set.getValue()) ) return set.getKey();
        }
        return null;
    }

    public String getHour(){
        return mHour;
    }

    public String getHourFull(){
        return mHour+":00";
    }

    public Double getSetpoint(){
        return mSetpoint;
    }

    public void setSetpoint(Double setpoint){
        mSetpoint = setpoint;
    }

    public static ArrayList<Program> createProgramsList(HashMap<String,HashMap<String,Double>> programsHashMap) {
        ArrayList<Program> programs = new ArrayList<Program>();
        for( HashMap.Entry<String, HashMap<String,Double>> day : programsHashMap.entrySet() ) {
            String keyDay = day.getKey();
            for( Map.Entry<String,Double> hour : day.getValue().entrySet() ) {
                String keyHour = hour.getKey();
                programs.add(new Program(keyDay,keyHour,hour.getValue()));
            };
        };
        return programs;
    }

    public static String[] getDays(){
        return new String[]{"Hétfő", "Kedd", "Szerda", "Csütörtök", "Péntek", "Szombat", "Vasárnap"};
    }

    public static String[] getHours(){
        return new String[]{
                "00:00", "01:00", "02:00", "03:00", "04:00", "05:00",
                "06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
                "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"
        };
    }

    @Override
    public String toString() {
        return "Program{" +
                "mDay='" + mDay + '\'' +
                ", mHour='" + mHour + '\'' +
                ", mSetpoint=" + mSetpoint +
                '}';
    }

    @Override
    public int compareTo(Program program) {
        if(getDayPlaceOfOlder() < program.getDayPlaceOfOlder()) return -1;
        if(getDayPlaceOfOlder() > program.getDayPlaceOfOlder()) return 1;
        int compare = getHour().compareTo(program.getHour());
        if( compare < 0 ) return -1;
        if( compare > 0 ) return 1;
        return 0;
    }
}


