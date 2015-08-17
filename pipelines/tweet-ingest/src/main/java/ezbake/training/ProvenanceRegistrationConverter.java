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


import org.apache.thrift.TException;

import ezbake.base.thrift.DateTime;
import ezbake.common.time.DateUtils;
import ezbake.frack.common.utils.thrift.ProvenanceRegistration;
import ezbake.frack.common.workers.IThriftConverter;
import ezbake.services.provenance.thrift.AgeOffMapping;



public class ProvenanceRegistrationConverter 
                    implements IThriftConverter<TweetWithRaw, ProvenanceRegistration> 
{
    private static final long serialVersionUID = 1L;

    private String uriPrefix = null;
    private long ruleId = 0;

    public void setUriPrefix(String uriPrefix) 
    {
        this.uriPrefix = uriPrefix;        
    }
    
    public void setRuleId(long ruleId)
    {
        this.ruleId = ruleId;
    }

    
    @Override
    public ProvenanceRegistration convert(TweetWithRaw tweetAndRaw) throws TException
    {
        //Build uri for data based on prefix and tweet
        String uri = this.uriPrefix + tweetAndRaw.getTweet().id;
        
        //get the tweet's epoch time
        long tweetEpoch = tweetAndRaw.getTweet().getTimestamp()*1000;
        
        //Specify a rule and an effective date for this tweet
        DateTime effectiveDateTime = DateUtils.getDateTimeFromUnixEpoch(tweetEpoch);                
        
        //Create mapping object with rule and effective date
        AgeOffMapping ageOffMapping = new AgeOffMapping(ruleId, effectiveDateTime);
        
        //Create registration object to be returned
        ProvenanceRegistration reg = new ProvenanceRegistration();
        
        //set values
        reg.uri = uri;        
        reg.ageOffRules.add(ageOffMapping);
        
        return reg;
    }    
    
}
