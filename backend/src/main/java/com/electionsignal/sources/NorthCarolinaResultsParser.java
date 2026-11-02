package com.electionsignal.sources;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;

/** Streaming parser for the official NC precinct TSV. No geographic or candidate IDs are inferred. */
public final class NorthCarolinaResultsParser {
    private static final int MAX_ARCHIVE=8*1024*1024,MAX_EXPANDED=32*1024*1024,MAX_ROWS=300000,MAX_LINE=4096;
    private static final String HEADER="County\tElection Date\tPrecinct\tContest Group ID\tContest Type\tContest Name\tChoice\tChoice Party\tVote For\tElection Day\tEarly Voting\tAbsentee by Mail\tProvisional\tTotal Votes\tReal Precinct";
    public record Row(String county,String precinct,String contestGroupId,String contestType,String contestName,String choice,String party,
                      Long electionDay,Long early,Long mail,Long provisional,Long total,boolean realPrecinct){}
    public record Summary(int rows,int counties,int nonzeroTotalRows){}
    private static String required(String x,String field){if(x.isBlank()||x.length()>300)throw new IllegalArgumentException("Invalid "+field);return x;}
    private static Long count(String x){if(x.isBlank())return null;if(!x.matches("[0-9]{1,15}"))throw new IllegalArgumentException("Invalid vote total");return Long.parseLong(x);}
    public Summary parse(byte[] bytes,Consumer<Row> consumer)throws IOException{
        if(bytes==null||bytes.length==0||bytes.length>MAX_ARCHIVE)throw new IllegalArgumentException("Invalid archive size");
        var counties=new HashSet<String>();int rows=0,nonzero=0;long expanded=0;
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)){
            var entry=zip.getNextEntry();if(entry==null||entry.isDirectory()||!"results_pct_20261103.txt".equals(entry.getName()))throw new IllegalArgumentException("Unexpected archive entry");
            var decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            var bounded=new FilterInputStream(zip){long size=0;private void add(int n)throws IOException{if(n>0&&(size+=n)>MAX_EXPANDED)throw new IOException("Expanded results exceed limit");}@Override public int read()throws IOException{int n=super.read();if(n>=0)add(1);return n;}@Override public int read(byte[] b,int off,int len)throws IOException{int n=in.read(b,off,len);add(n);return n;}};
            var reader=new BufferedReader(new InputStreamReader(bounded,decoder));String line=reader.readLine();if(line==null||!line.replace("\uFEFF","").equals(HEADER))throw new IllegalArgumentException("Unexpected results header");expanded+=line.length()+1;
            while((line=reader.readLine())!=null){expanded+=line.length()+1;if(expanded>MAX_EXPANDED||line.length()>MAX_LINE||++rows>MAX_ROWS)throw new IllegalArgumentException("Results payload exceeds bounds");
                String[] f=line.split("\\t",-1);if(f.length!=15||!"11/03/2026".equals(f[1]))throw new IllegalArgumentException("Unexpected result scope");
                String county=required(f[0],"county"),precinct=required(f[2],"precinct"),contest=required(f[3],"contest id"),name=required(f[5],"contest name"),choice=required(f[6],"choice");
                if(!contest.matches("[0-9]{1,12}")||!f[8].matches("[0-9]{1,3}")||!Set.of("Y","N").contains(f[14]))throw new IllegalArgumentException("Invalid contest or precinct attributes");
                Long electionDay=count(f[9]),early=count(f[10]),mail=count(f[11]),provisional=count(f[12]),total=count(f[13]);
                if(total!=null&&total>0)nonzero++;counties.add(county);
                consumer.accept(new Row(county,precinct,contest,f[4],name,choice,f[7],electionDay,early,mail,provisional,total,f[14].equals("Y")));
            }
            if(zip.getNextEntry()!=null)throw new IllegalArgumentException("Multiple archive entries are not supported");
        }
        return new Summary(rows,counties.size(),nonzero);
    }
}
