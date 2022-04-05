package com.bookanapp.employee.services;

import com.bookanapp.employee.entities.rest.Provider;
import com.bookanapp.employee.services.helpers.Forms;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DateRangeService {
    public String addZeroBeforeInt(int toAdd){
        if (String.valueOf(toAdd).length()<2)
            return "0"+toAdd;
        return String.valueOf(toAdd);

    }

    public List<LocalDate> dateRange(String start, String end) {
        String[] parseDateStart=deleteT(start).split("-");
        String[] parseDateEnd=deleteT(end).split("-");
        List<LocalDate> dateRange=new ArrayList<>();
        int endDate = Integer.parseInt(parseDateEnd[2]);
        for (LocalDate date = LocalDate.of(Integer.parseInt(parseDateStart[0]), Integer.parseInt(parseDateStart[1]), Integer.parseInt(parseDateStart[2]));
             !date.isAfter(LocalDate.of(Integer.parseInt(parseDateEnd[0]), Integer.parseInt(parseDateEnd[1]), endDate
             )); date=date.plusDays(1)){
            dateRange.add(date);
        }

        return dateRange;
    }

    public List<LocalDate> enhancedDateRange(LocalDate start, LocalDate end) {
        var dates = start.datesUntil(end).collect(Collectors.toList());
        dates.add(end);
        return dates;
    }


    public String deleteT(String source){
        String [] parseDate=source.split("T");
        return parseDate[0];
    }

    public List<LocalDate> dateRangeUTC(String start, String end, String offset) {

        int parsedOffset = -(Integer.parseInt(offset)*60);
        LocalDate startDate = Instant.parse(start).atOffset(ZoneOffset.ofTotalSeconds(parsedOffset)).toLocalDate();
        LocalDate endDate = Instant.parse(end).atOffset(ZoneOffset.ofTotalSeconds(parsedOffset)).toLocalDate();

        List<LocalDate> dateRange=new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date=date.plusDays(1)){
            dateRange.add(date);
        }
        return dateRange;
    }


    /**
     * Sample input: 3/2; interval array of length 13
     * We start by defining our pivot as 3 and adding the first three elements to the list:
     * interval.get(0), interval.get(1), interval.get(2).
     * Then, we reach the iteration index 3 and 3/3 > 0 is true, which means we have to set our pivot further and skip the off days.
     * Therefore, the next pivot position will be pivotOnDays = onDays + offDays
     * and the iteration index will be equal to index += offDays.
     * In this case, we will have pivotOnDays = 8 and index = 5. We add
     * interval.get(5), interval.get(6), interval.get(7).
     * 8/8 > 0 and we have to reupdate our index, and so on.
     *
     * @param interval
     * @param pattern
     * @return
     */
    public List<LocalDate> rosterPatternDateRange(List<LocalDate> interval, String pattern) {

        String[] parsePattern = pattern.split("/");

        int onDays = Integer.parseInt(parsePattern[0]);
        int offDays = Integer.parseInt(parsePattern[1]);
        int intervalSize = interval.size();

        Collections.sort(interval);
        // LinkedList is faster for add and remove operations: https://www.programcreek.com/2013/03/arraylist-vs-linkedlist-vs-vector/
        List<LocalDate> onDaysList = new LinkedList<>();

        int pivotOnDays = onDays;
        for (int i = 0; i < intervalSize; i++) {
            if (i / pivotOnDays > 0) {
                pivotOnDays += onDays + offDays;
                // Has to be -1 because the next iteration will increment the value of i
                i += offDays - 1;
            } else {
                onDaysList.add(interval.get(i));
            }
        }

        return onDaysList;
    }

    public List<LocalTime> addHour(String opening, String closure, String duration, String scheduleType, Provider provider) {
        DateTimeFormatter timeFormatter;
        Locale locale = this.getProviderLocale(provider.getLocale());
        if (locale.equals(new Locale("en"))){
            timeFormatter=DateTimeFormatter.ofPattern("h:mm a");
        } else {
            timeFormatter = DateTimeFormatter.ofPattern("H:mm");
        }

        LocalTime start = LocalTime.parse(opening, timeFormatter);
        LocalTime end = LocalTime.parse(closure, timeFormatter);

        return getLocalTimes(duration, scheduleType, start, end);
    }


    private List<LocalTime> getLocalTimes(String duration, String scheduleType, LocalTime start, LocalTime end) {
        List<LocalTime> localTimes = new ArrayList<>();

        long avaialableMinutes= Duration.between(start, end).toMinutes();

        localTimes.add(start);
        long durationInMinutes;

        if (duration.contains(":")) {
            durationInMinutes = transformTimeToDuration(duration);
        } else {
            durationInMinutes = Long.parseLong(duration);
        }

        for (long i = 0L; i < avaialableMinutes; i++) {
            LocalTime time = localTimes.get(localTimes.size() - 1).plusMinutes(durationInMinutes);
            if (!localTimes.contains(time)) {
                if (localTimes.contains(end) || time.isAfter(end) || time.plusMinutes(durationInMinutes).isAfter(end)) {
                    localTimes.remove(end);
                    localTimes.remove(time);
                    break;
                }
                if (!scheduleType.equals("1"))
                    localTimes.add(time);
            }

        }


        return localTimes;
    }

    public long transformTimeToDuration(String time){
        var splitTime =time.split(":");
        long hours=Long.parseLong(splitTime[0])*60;
        long minutes=Long.parseLong(splitTime[1]);
        return hours+minutes;
    }

    public Locale getProviderLocale (String lang) {
        Locale locale;
        if (lang.contains("-")) {
            String[] parsedLocale = lang.split("-");
            locale = new Locale(parsedLocale[0], parsedLocale[1]);
        } else {
            locale = new Locale(lang);
        }

        return locale;
    }
}
