package com.ef;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TOREMOVE
{

    public static void main(String[] args)
    {
        String date = "2017-01-01 00:00:11.763";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime localDateTime = LocalDateTime.parse(date, formatter);

        System.out.println(localDateTime);

        System.out.println(formatter.format(localDateTime));
    }

}
