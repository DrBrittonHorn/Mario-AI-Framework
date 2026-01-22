package agents;

import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import mff.agents.common.IMarioAgentMFF;
import mff.agents.common.MarioTimerSlim;
import mff.forwardmodel.common.Converter;
import mff.forwardmodel.slim.core.MarioForwardModelSlim;

/**
 * Adapter that wraps an IMarioAgentMFF agent to work with the original MarioAgent interface.
 * This allows MFF agents to be used with MarioGame.
 */
public class MFFAgentAdapter implements MarioAgent {
    private final IMarioAgentMFF mffAgent;
    private static final int LEVEL_CUTOUT_TILE_WIDTH = 27;

    public MFFAgentAdapter(IMarioAgentMFF mffAgent) {
        this.mffAgent = mffAgent;
    }

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        MarioForwardModelSlim slimModel = Converter.originalToSlim(model, LEVEL_CUTOUT_TILE_WIDTH);
        mffAgent.initialize(slimModel);
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        MarioForwardModelSlim slimModel = Converter.originalToSlim(model, LEVEL_CUTOUT_TILE_WIDTH);
        MarioTimerSlim slimTimer = new MarioTimerSlim(timer.getRemainingTime());
        return mffAgent.getActions(slimModel, slimTimer);
    }

    @Override
    public String getAgentName() {
        return mffAgent.getAgentName();
    }
}
