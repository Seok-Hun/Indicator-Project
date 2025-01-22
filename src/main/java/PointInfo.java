public class PointInfo {
    /**
     * 지점명
     */
    private final String pointName;

    /**
     * 월간 게시물 수
     */
    private final int monthlyPosts;

    /**
     * 팔로워 수
     */
    private final int followers;

    /**
     * 평균 좋아요 수
     */
    private final int likesAve;

    /**
     * 평균 댓글 수
     */
    private final int commentsAve;

    /**
     * 인스타 참여도 = (평균 좋아요 수+평균 댓글 수)/팔로워 수
     */
    private final double participation;

    private PointInfo(PointInfoBuilder PB) {
        this.pointName = PB.pointName;
        this.monthlyPosts = PB.monthlyPosts;
        this.followers = PB.followers;
        this.likesAve = PB.likesAve;
        this.commentsAve = PB.commentsAve;
        this.participation = PB.participation;
    }

    @Override
    public String toString() {
        return String.format("%-8s %-8d %-8d %-8d %-8d %-8.2f",pointName,monthlyPosts,followers,likesAve,commentsAve,participation*100);
    }

    public String[] toArray() {
        return new String[]{
                pointName,
                String.valueOf(monthlyPosts),
                String.valueOf(followers),
                String.valueOf(likesAve),
                String.valueOf(commentsAve),
                String.format("%.2f", participation*100)
        };
    }

    public static class PointInfoBuilder{
        private String pointName;
        private int monthlyPosts;
        private int followers;
        private int likesAve;
        private int commentsAve;
        private double participation;

        public PointInfoBuilder pointName(String pointName) {
            this.pointName = pointName;
            return this;
        }

        public PointInfoBuilder monthlyPosts(int monthlyPosts) {
            this.monthlyPosts = monthlyPosts;
            return this;
        }

        public PointInfoBuilder followers(int followers) {
            this.followers = followers;
            return this;
        }

        public PointInfoBuilder likesAve(int likeSum) {
            try {
                this.likesAve = likeSum / monthlyPosts;
            } catch (ArithmeticException e){
                this.likesAve = 0;
            }
            return this;
        }

        public PointInfoBuilder commentsAve(int commentSum) {
            try {
                this.commentsAve = commentSum / monthlyPosts;
            } catch (ArithmeticException e){
                this.commentsAve = 0;
            }
            return this;
        }

        public PointInfo build(){
            this.participation = ((double)likesAve+commentsAve)/followers;
            return new PointInfo(this);
        }
    }
}
