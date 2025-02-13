package syncMonitor.view;


import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import lombok.Getter;
import syncMonitor.config.wrapper.MonitorConfig;
import syncMonitor.topology.Topology;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class View{
    @Getter
    private final List<Topology> topologyList;

    private static Integer SIZE = 11;

    public View(List<Topology> topologyList , MonitorConfig monitorConfig) {
        this.topologyList = topologyList;
        if(monitorConfig.getMode().chars().allMatch(Character::isDigit)){
            SIZE = Integer.parseInt(monitorConfig.getMode());
        }else if ("WIDE".equalsIgnoreCase(monitorConfig.getMode())) {
            SIZE = 90;
        }
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


            for (Topology topology : topologyList) {
                String targetCommitTime = "null";
                try{
                    targetCommitTime = topology.runTargetTimeQuery();
                }catch (Exception e){
                    targetCommitTime = "null";
                }
                // 데이터 조회
                try{
                    String sourceCn = topology.runSourceChangeNumberQuery();
                    String targetCn = topology.runTargetChangeNumberQuery();
                    String sourceRes = String.valueOf(sourceCn);
                    String targetRes = String.valueOf(targetCn);
                    String diffRes = String.valueOf(Long.parseLong(sourceCn) - Long.parseLong(targetCn));
                    if(sourceCn== null || sourceCn.length() == 0){
                        sourceRes = "source fail";
                    }
                    if(targetCn == null || targetCn.length() == 0){
                        targetRes = "target fail";
                    }
                    if(targetCn == null || sourceCn== null){
                        diffRes = "fail";
                    }
                    asciiTable.addRow(topology.getTopologyName() == null ? "null" : topology.getTopologyName(),
                            topology.getProSyncUser(), sourceRes, targetRes, diffRes, targetCommitTime);
                    asciiTable.addRule();
                }catch (Exception e){
                    String sourceCn = topology.getTopologyName()+" src fail";
                    String targetCn = topology.getTopologyName()+" target fail";
                    asciiTable.addRow(topology.getTopologyName() == null ? "null":topology.getTopologyName(),
                            topology.getProSyncUser(), sourceCn, targetCn, "Fail get res");
                    asciiTable.addRule();
                }

//                asciiTableTime.addRow(topology.getTopologyName()+" commit to target at" , targetCommitTime);
//                asciiTableTime.addRule();

            }

            // 테이블 출력
            CWC_LongestLine cwc = new CWC_LongestLine();
            asciiTable.setTextAlignment(TextAlignment.CENTER);
            cwc.add(SIZE, 0).add(SIZE,0).add(SIZE,0).add(SIZE,0).add(SIZE,0);
            asciiTable.getRenderer().setCWC(cwc);
//            asciiTable.getContext().setWidth(SIZE);
            System.out.println(asciiTable.render());

//            asciiTableTime.setTextAlignment(TextAlignment.CENTER);
//            System.out.println(asciiTableTime.render(SIZE));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
