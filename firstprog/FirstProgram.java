import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import hipi.image.FloatImage;
import hipi.image.ImageHeader;
import hipi.imagebundle.mapreduce.ImageBundleInputFormat;
import hipi.util.ByteUtils;
import hipi.imagebundle.mapreduce.HipiJob;
import hipi.imagebundle.mapreduce.output.BinaryOutputFormat;

import java.io.IOException;

public class FirstProgram extends Configured implements Tool {
    public static class MyMap extends Mapper<ImageHeader, FloatImage, IntWritable, FloatImage> {
      public void map(ImageHeader key, FloatImage value, Context context)
        throws IOException, InterruptedException {
        if (value != null && value.getWidth() > 1 && value.getHeight() > 1 && value.getBands() == 3) {
          FloatImage avg = new FloatImage(1, 1, 3);
          float[] avgData = avg.getData();
          float[] valData = value.getData();
          for (int i = 0; i < value.getWidth(); i++) {
            for (int j = 0; j < value.getHeight(); j++) {
              avgData[0] += valData[i * value.getHeight() * 3 + j * 3];
              avgData[1] += valData[i * value.getHeight() * 3 + j * 3 + 1];
              avgData[2] += valData[i * value.getHeight() * 3 + j * 3 + 2];
            }
          }
          avg.scale(1.0f / (value.getWidth() * value.getHeight()));
          context.write(new IntWritable(0), avg);
        }
      }
    }

    public static class MyReduce extends Reducer<IntWritable, FloatImage, IntWritable, FloatImage> {
      public void reduce(IntWritable key, Iterable<FloatImage> values, Context context)
        throws IOException, InterruptedException {
        FloatImage avg = new FloatImage(1, 1, 3);
        int total = 0;
        for (FloatImage val : values) {
          avg.add(val);
          total++;
        }
        if (total > 0) {
          avg.scale(1.0f / total);
          context.write(key, avg);
        }
      }
    }

    public int run(String[] args) throws Exception {
      HipiJob job = new HipiJob(getConf(), "FirstProgram");
      job.setJarByClass(FirstProgram.class);
      job.setOutputKeyClass(IntWritable.class);
      job.setOutputValueClass(FloatImage.class);

      job.setMapperClass(MyMap.class);
      job.setCombinerClass(MyReduce.class);
      job.setReducerClass(MyReduce.class);

      //String inputFileType = args[2];
      job.setOutputFormatClass(BinaryOutputFormat.class);
      System.out.println(args[0]);
      FileInputFormat.setInputPaths(job, new Path(args[0]));
      FileOutputFormat.setOutputPath(job, new Path(args[1]));

      boolean success = job.waitForCompletion(true);
      return success ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
      ToolRunner.run(new FirstProgram(), args);
      System.exit(0);
    }
  }
