
/**
    Class PacketNode handles invididual DatagramPacket state information
    which will be used to facilitate window management for our SelectiveRepeat
    protocol. It will be implemented in client and receiver program as elements
    of an ArrayBlockingQueue.
*/
public class PacketNode {

    // ENUM states for this specified packet
    public enum State {
        READY(1), SENT(2), ACKED(3), RECEIVED(4), LOST(5), CORRUPTED(6), RESENT(7), NAKD(8);
        protected int value; // the value of enum state

        /**
         * the constructor of state
         * @param value the value of enum state
         */
        private State(int value) {
            this.value = value;
        }

        /**
         * State class override toString method.
         * @return the string
         */
        public String toString() {
            switch (this.value){
                case 1 : return "READY";
                case 2 : return "SENT";
                case 3 : return "ACKED";
                case 4 : return "RECEIVED";
                case 5 : return "LOST";
                case 6 : return "CORRUPTED";
                case 7 : return "RESENT";
                case 8 : return "NAKD";
            }
            return "";
        }
    }

    private int seqNum;
    private State state;

    // CONSTRUCTOR
    public PacketNode(int seqNum, int stateValue) {
        this.seqNum = seqNum;
        this.state.value = stateValue;
    }

    /** BEGIN GETTERS/SETTERS */
    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public String getStateString() {
         return this.state.toString();
    }

    public int getStateValue() {
        return this.state.value;
    }

    public void setState(int state) {
        this.state.value = state;
    }

}
