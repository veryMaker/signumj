package signumj.entity.response;

import signumj.entity.SignumValue;
import signumj.entity.response.http.SuggestFeeResponse;

public class FeeSuggestion {
    private final SignumValue cheapFee;
    private final SignumValue standardFee;
    private final SignumValue priorityFee;

    public FeeSuggestion(SignumValue cheapFee, SignumValue standardFee, SignumValue priorityFee) {
        this.cheapFee = cheapFee;
        this.standardFee = standardFee;
        this.priorityFee = priorityFee;
    }

    public FeeSuggestion(SuggestFeeResponse suggestFeeResponse) {
        this.cheapFee = SignumValue.fromPlanck(suggestFeeResponse.getCheap());
        this.standardFee = SignumValue.fromPlanck(suggestFeeResponse.getStandard());
        this.priorityFee = SignumValue.fromPlanck(suggestFeeResponse.getPriority());
    }

    public SignumValue getCheapFee() {
        return cheapFee;
    }

    public SignumValue getStandardFee() {
        return standardFee;
    }

    public SignumValue getPriorityFee() {
        return priorityFee;
    }
}
