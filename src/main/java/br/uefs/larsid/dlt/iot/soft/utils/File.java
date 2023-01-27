/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.uefs.larsid.dlt.iot.soft.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Emers
 */
public class File {

    public static void write(String fileName, String fileExtension, String data) throws IOException {
        FileWriter file = new FileWriter(fileName + "." + fileExtension);
        PrintWriter printWriter = new PrintWriter(file);

        printWriter.print(data);
        file.close();
    }
}
