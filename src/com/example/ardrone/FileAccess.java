package com.example.ardrone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;
import android.util.Log;

public class FileAccess
{
	File rootFile;
	File file;
	
	FileWriter fileWriter;
	BufferedWriter bufferedWriter;
	
	public FileAccess(String filename)
	{
		rootFile = Environment.getExternalStorageDirectory();
		file = new File(rootFile, filename);
		
		try
		{
			fileWriter = new FileWriter(file);
			bufferedWriter = new BufferedWriter(fileWriter);
		}
		catch (IOException e)
		{
			Log.d("plik", "FileAccess: fileAccess ERR");
		}		
	}
	
    public void append(String s)
    {
        if (rootFile.canWrite())
        {
            try
            {
            	bufferedWriter.append(s + "\n");
                Log.d("plik", "write OK");
            } catch (IOException e)
            {

                Log.d("plik", "Append: fileAccess ERR");
            }
        }
    }
    
    public void closeFile()
    {
        try
        {
        	bufferedWriter.close();
            Log.d("plik", "close OK");
        } catch (IOException e)
        {

            Log.d("plik", "closeFile: fileAccess ERR");
        }
    }
    


}
