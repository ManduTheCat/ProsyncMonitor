package syncMonitor.view;


import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import syncMonitor.config.wrapper.MonitorConfig;
import syncMonitor.topology.Topology;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class View{
    @Getter
    private final List<Topology> topologyList;

    private static Integer SIZE = 11;

    public View(List<Topology> topologyList , MonitorConfig monitorConfig) {
        this.topologyList = topologyList;
        if(monitorConfig.getMode().chars().allMatch(Character::isDigit)){
            SIZE = Integer.parseInt(monitorConfig.getMode());
            log.info("use external SIZE config");
        }else if ("WIDE".equalsIgnoreCase(monitorConfig.getMode())) {
            SIZE = 15;
            log.info("Setting WIDE is checked use WIDE size :" + SIZE);
        }
        log.info("column width SIZE is : " + SIZE);
    }
    public void genView() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 제목 출력
            AsciiTable asciiTable = new AsciiTable();
            asciiTable.addRule();
            asciiTable.addRow(null,null, null, null,"Prosync Monitor" ,
                    "Last Commit Time" );
            asciiTable.addRule();
            asciiTable.addRow("TOPOLOGY", "PSYC ID", "SOURCE TSN", "TARGET TSN", "TSN GAP",
                    LocalDateTime.now().format(formatter) );
            asciiTable.addRule();
            Map<String, File> fileMap = new HashMap<>();
            
            for (Topology topology: topologyList){
//                String fileName = topology.getTopologyName();
//                String dir = File.separator+fileName;
//                fileMap.put(fileName, new File(dir));
            }

            for (Topology topology : topologyList) {
                String targetCommitTime = "null";
                try{
                    targetCommitTime = topology.runTargetTimeQuery();
                }catch (Exception e){
                    log.error("fail get query result");
                    log.error(e.fillInStackTrace().toString());
                    targetCommitTime = "null";
                }
                // 데이터 조회
                try{
                    String sourceCn = topology.runSourceChangeNumberQuery();
                    String targetCn = topology.runTargetChangeNumberQuery();
                    String sourceRes = String.valueOf(sourceCn);
                    String targetRes = String.valueOf(targetCn);
                    Long gapDiff = Long.parseLong(sourceCn) - Long.parseLong(targetCn);
                    String diffString = String.valueOf(gapDiff);
                    File gaplogFile = fileMap.get(topology.getTopologyName());
                    try(BufferedWriter bw =  new BufferedWriter(new FileWriter(gaplogFile.getPath()))){
                        bw.write(LocalDateTime.now().format(formatter) + ": " + topology.getTopologyName() + " gap" + diffString );
                    }
                    if(sourceCn== null || sourceCn.length() == 0){
                        log.error("source TSN or SCN is empty ");
                        sourceRes = "source fail";
                    }
                    if(targetCn == null || targetCn.length() == 0){
                        log.error("target TSN or SCN is empty ");
                        targetRes = "target fail";
                    }
                    if(targetCn == null || sourceCn== null){
                        diffString = "fail";
                    }
                    if(topology.getTopologyName()== null){
                        log.error("topology name is null check config");
                    }
                    asciiTable.addRow(topology.getTopologyName() == null ? "null" : topology.getTopologyName(),
                            topology.getProSyncUser(), sourceRes, targetRes, diffString, targetCommitTime);
                    asciiTable.addRule();
                }catch (Exception e){
                    String sourceCn = topology.getTopologyName()+" src fail";
                    String targetCn = topology.getTopologyName()+" target fail";
                    asciiTable.addRow(topology.getTopologyName() == null ? "null":topology.getTopologyName(),
                            topology.getProSyncUser(), sourceCn, targetCn, "Fail get res");
                    asciiTable.addRule();
                }
            }

            // 테이블 출력
            CWC_LongestLine cwc = new CWC_LongestLine();
            asciiTable.setTextAlignment(TextAlignment.CENTER);
            cwc.add(SIZE, 0).add(SIZE,0).add(SIZE,0).add(SIZE,0).add(SIZE,0);
            asciiTable.getRenderer().setCWC(cwc);
            System.out.println(asciiTable.render());

        } catch (Exception e) {
            log.error(e.fillInStackTrace().toString());
        }
    }

}
