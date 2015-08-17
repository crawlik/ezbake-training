/*   Copyright (C) 2013-2014 Computer Sciences Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package ezbake.training;



import ezbake.frack.api.Pipeline;
import ezbake.frack.api.PipelineBuilder;
import ezbake.frack.common.utils.INSUtil;
import ezbake.frack.common.utils.INSUtil.INSInfo;
import ezbake.frack.common.workers.BroadcastWorker;
import ezbake.frack.common.workers.SSRBroadcastWorker;
import ezbake.frack.common.workers.WarehausWorker;
import ezbake.services.provenance.thrift.AgeOffRule;
import ezbake.frack.common.workers.ProvenanceWorker;
import ezbake.frack.common.utils.ProvenanceUtils;
import ezbake.frack.common.workers.PassThruThriftConverter;

public class TweetIngestBuilderWithProvenance implements PipelineBuilder {
    public static final String FEED_NAME = "tweet-ingest";

    
    
    public Pipeline build() {
        Pipeline pipeline = new Pipeline();
        TweetIngestGenerator generator = new TweetIngestGenerator();
        TweetIngestParser parser = new TweetIngestParser();

        //ins client object
        INSInfo insInfo = INSUtil.getINSInfo(pipeline, FEED_NAME);
 
        
        //get or create rule if does not exist
        long ruleId = 0;
        String ruleName = "20DayRule";
        
        AgeOffRule rule = ProvenanceUtils.GetRule(ruleName, pipeline.getProperties());
        
        if (rule != null)
        {
            ruleId = rule.id;            
        }
        else
        {
            //If not found, then create "20DayRule"
            long retentionDurationSeconds = 1728000; // 20 days
            int maximumExecutionPeriod = 5; //5 days
            ruleId = ProvenanceUtils.CreateRule(ruleName, 
                                                 retentionDurationSeconds, 
                                                 maximumExecutionPeriod, 
                                                 pipeline.getProperties());            
        }
        
        
        //pravenance registration converter
        ProvenanceRegistrationConverter regConverter = new ProvenanceRegistrationConverter();
        regConverter.setRuleId(ruleId);
        regConverter.setUriPrefix(insInfo.getUriPrefix());        
        
        //convert to repository
        RepositoryConverter repoConverter = new RepositoryConverter();
        repoConverter.setUriPrefix(insInfo.getUriPrefix());
        
        //worker writes to the repository
        WarehausWorker<TweetWithRaw> warehausWorker = 
                new WarehausWorker<>(
                        TweetWithRaw.class, 
                        repoConverter, 
                        new VisibilityConverter());
        
        //converter to standard search result object
        SSRConverter ssrConverter = new SSRConverter();
        ssrConverter.setUriPrefix(insInfo.getUriPrefix());
        
        
        //worker writes to the queue with topics according to the INS
        
        BroadcastWorker<TweetWithRaw> broadcastWorker = 
                new BroadcastWorker<>(
                        TweetWithRaw.class, 
                        insInfo.getTopics(), 
                        new PassThruThriftConverter());
        
        
        
        //worker writes to the queue with SSR topic
        SSRBroadcastWorker<TweetWithRaw> ssrWorker =
                new SSRBroadcastWorker<>(
                        TweetWithRaw.class, 
                        ssrConverter);
        
//        //worker writes to the queue with topics according to the INS
//        BroadcastWorker<TweetWithRaw> broadcastWorker = 
//                new BroadcastWorker<>(
//                        TweetWithRaw.class, 
//                        insInfo.getTopics(), 
//                        new TweetBroadcastConverter());

        //ProvenanceWorker writes entries to Provenance Service
        ProvenanceWorker<TweetWithRaw> provenanceWorker = 
                new ProvenanceWorker<>(TweetWithRaw.class, regConverter);
        
        
        pipeline.addGenerator(FEED_NAME + "_generator", generator);
        
        pipeline.addWorker(FEED_NAME + "_parser", parser);
        pipeline.addWorker(FEED_NAME + "_warehaus_worker", warehausWorker);
        pipeline.addWorker(FEED_NAME + "_broadcast_worker", broadcastWorker);
        pipeline.addWorker(FEED_NAME + "_ssr_worker", ssrWorker);
        pipeline.addWorker(FEED_NAME + "_provenance_worker", provenanceWorker);
        

        pipeline.addConnection(FEED_NAME + "_generator", FEED_NAME + "_parser");
        pipeline.addConnection(FEED_NAME + "_parser", FEED_NAME + "_warehaus_worker");
        pipeline.addConnection(FEED_NAME + "_warehaus_worker", FEED_NAME + "_broadcast_worker");
        pipeline.addConnection(FEED_NAME + "_warehaus_worker", FEED_NAME + "_ssr_worker");
        pipeline.addConnection(FEED_NAME + "_warehaus_worker", FEED_NAME + "_provenance_worker");

        return pipeline;
    }
}
