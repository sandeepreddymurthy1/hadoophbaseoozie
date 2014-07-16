package com.webanalytics.mapreduce.complete;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

import com.webanalytics.dto.WebCollectionDTO;
import com.webanalytics.hbase.model.AnalyticTableConstant;
import com.webanalytics.hbase.model.RawDataTable;
import com.webanalytics.util.DateHelper;
import com.webanalytics.util.JAXBContextHelper;

import eu.bitwalker.useragentutils.UserAgent;

public class BrowserAccess {

	public static class Mapper extends
			TableMapper<ImmutableBytesWritable, Text> {

		@Override
		protected void setup(Context context) {

		}

		@Override
		protected void map(ImmutableBytesWritable rowkey, Result result,
				Context context) throws IOException, InterruptedException {
			NavigableMap<byte[], byte[]> columnMap = result
					.getFamilyMap(AnalyticTableConstant.BROWSER_COLUMN_FAMILY);
			String dbKey = rowkey.toString().split(",")[0];

			for (Entry<byte[], byte[]> entry : columnMap.entrySet()) {
				
				ImmutableBytesWritable ibKey = new ImmutableBytesWritable(dbKey.getBytes());
				//context.write(ibKey, val);
			}

		}
	}

	public static class Reduce
			extends
			TableReducer<ImmutableBytesWritable, Text, ImmutableBytesWritable> {

		@Override
		protected void reduce(ImmutableBytesWritable rowkey,
				Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			Put put = new Put(rowkey.get());
			Map<String, Integer> result = new HashMap<String, Integer>();
			for (Text temp : values) {
				String str = temp.toString();
				Integer count = result.get(str);
				if( count == null ){
					result.put(str, new Integer(1));
				}else{
					result.put(str, new Integer(count.intValue()+1));
				}
			}
			for (Entry<String, Integer> entry : result.entrySet()) {
				put.add(AnalyticTableConstant.BROWSER_COLUMN_FAMILY,
						Bytes.toBytes(entry.getKey()),
						Bytes.toBytes(entry.getValue()));
			}
			ImmutableBytesWritable ibKey = new ImmutableBytesWritable(rowkey);
			context.write(ibKey, put);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = HBaseConfiguration.create();
		Job job = new Job(conf, "Browser Access Total");
		job.setJarByClass(BrowserAccess.class);

		Scan scan = new Scan();
		scan.addFamily(AnalyticTableConstant.BROWSER_COLUMN_FAMILY);
		TableMapReduceUtil.initTableMapperJob(
				Bytes.toString(AnalyticTableConstant.DAILY_TABLE_NAME), scan, Mapper.class,
				ImmutableBytesWritable.class, Text.class, job);
		TableMapReduceUtil.initTableReducerJob(
				Bytes.toString(AnalyticTableConstant.COMPLETE_TABLE_NAME), Reduce.class,
				job);
		// job.setNumReduceTasks(0);
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}