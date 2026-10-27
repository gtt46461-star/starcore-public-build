package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Executable SU1 research-language reference, not an Android integration or an AI trainer.
 * All values are immutable; numbers/positions are 1..10, SET10 uses bits 0..9.
 * History identifiers are consecutive ordinals, NOT decimal lottery issue identifiers.
 * SCORE10 and PROB10 are distinct. A normalized probability is not a calibration claim.
 */
public final class TypedProgram {
    public enum Type { SCALAR, BOOL, NUMBER, POSITION, SET10, SCORE10, PROB10 }
    public enum Phase { PREDICT, SETTLE }
    public enum Op {
        CONST, PARAM, READ_NUMBER, LOCATE_NUMBER, SETTLED_NUMBER, STATE_READ,
        TO_SCALAR, EQ, NE, LESS, ADD, SUBTRACT, MULTIPLY, IF,
        SET_UNION, SET_INTERSECTION, SET_DIFFERENCE, SET_COMPLEMENT,
        SET_CONTAINS, SET_COUNT, SINGLETON, SET_TO_SCORE,
        SCORE_ADD, SCORE_SCALE, PROB_TO_SCORE, SOFTMAX, PROB_MIX, TOP3,
        CALL, SET_WHERE, SCORE_EACH_NUMBER
    }

    public static final class Value {
        public final Type type;
        private final double scalar;
        private final double[] vector;
        private Value(Type type, double scalar, double[] vector) {
            this.type = Objects.requireNonNull(type);
            this.scalar = scalar == 0 ? 0 : scalar;
            this.vector = vector == null ? null : vector.clone();
            if (this.vector != null) {
                require((type == Type.SCORE10 || type == Type.PROB10) && this.vector.length == 10, "VALUE_VECTOR");
                double sum = 0;
                for (int i = 0; i < 10; i++) {
                    require(Double.isFinite(this.vector[i]), "VALUE_NONFINITE");
                    if (this.vector[i] == 0) this.vector[i] = 0;
                    if (type == Type.PROB10) require(this.vector[i] >= 0 && this.vector[i] <= 1, "PROB_RANGE");
                    sum += this.vector[i];
                }
                if (type == Type.PROB10) require(Math.abs(sum - 1) <= 1e-10, "PROB_SUM");
            } else {
                require(type != Type.SCORE10 && type != Type.PROB10 && Double.isFinite(scalar), "VALUE_SCALAR");
                if (type == Type.BOOL) require(scalar == 0 || scalar == 1, "BOOL_RANGE");
                if (type == Type.NUMBER || type == Type.POSITION)
                    require(scalar == Math.rint(scalar) && scalar >= 1 && scalar <= 10, "ENTITY_RANGE");
                if (type == Type.SET10) require(scalar == Math.rint(scalar) && scalar >= 0 && scalar <= 1023, "SET_RANGE");
            }
        }
        public static Value scalar(double x) { return new Value(Type.SCALAR, x, null); }
        public static Value bool(boolean x) { return new Value(Type.BOOL, x ? 1 : 0, null); }
        public static Value number(int x) { return new Value(Type.NUMBER, x, null); }
        public static Value position(int x) { return new Value(Type.POSITION, x, null); }
        public static Value set(int mask) { return new Value(Type.SET10, mask, null); }
        public static Value score(double... x) { return new Value(Type.SCORE10, 0, x); }
        public static Value probability(double... x) { return new Value(Type.PROB10, 0, x); }
        public double scalar() { require(vector == null, "NOT_SCALAR"); return scalar; }
        public int integer() { require(type == Type.NUMBER || type == Type.POSITION || type == Type.SET10, "NOT_ENTITY_OR_SET"); return (int)scalar; }
        public boolean bool() { require(type == Type.BOOL, "NOT_BOOL"); return scalar == 1; }
        public double[] vector() { require(vector != null, "NOT_VECTOR"); return vector.clone(); }
        private String canonical() {
            StringBuilder b = new StringBuilder(type.name());
            if (vector == null) return b.append(':').append(Double.toHexString(scalar)).toString();
            for (double x : vector) b.append(':').append(Double.toHexString(x));
            return b.toString();
        }
    }

    public static final class Node {
        public final String id, ref;
        public final Op op;
        public final Type type;
        public final int index;
        private final List<String> inputs;
        private final Value literal;
        private Node(String id, Op op, Type type, int index, String ref, Value literal, String... inputs) {
            identifier(id); this.id = id; this.op = Objects.requireNonNull(op);
            this.type = Objects.requireNonNull(type); this.index = index; this.ref = ref == null ? "" : ref;
            this.literal = literal; this.inputs = Collections.unmodifiableList(Arrays.asList(inputs.clone()));
            for (String input : this.inputs) identifier(input);
            if (!this.ref.isEmpty()) identifier(this.ref);
        }
        public static Node of(String id, Op op, Type type, String... inputs) { return new Node(id, op, type, 0, "", null, inputs); }
        public static Node constant(String id, Value v) { return new Node(id, Op.CONST, v.type, 0, "", v); }
        public static Node parameter(String id, Type type, int at) { return new Node(id, Op.PARAM, type, at, "", null); }
        public static Node history(String id, String position, int lag) { return new Node(id, Op.READ_NUMBER, Type.NUMBER, lag, "", null, position); }
        public static Node locate(String id, String number, int lag) { return new Node(id, Op.LOCATE_NUMBER, Type.POSITION, lag, "", null, number); }
        public static Node state(String id, int slot) { return new Node(id, Op.STATE_READ, Type.SCALAR, slot, "", null); }
        public static Node call(String id, String module, Type type, String... arguments) { return new Node(id, Op.CALL, type, 0, module, null, arguments); }
        public static Node bind(String id, Op op, String module, String... captured) {
            require(op == Op.SET_WHERE || op == Op.SCORE_EACH_NUMBER, "BIND_OP");
            return new Node(id, op, op == Op.SET_WHERE ? Type.SET10 : Type.SCORE10, 0, module, null, captured);
        }
        public Node withOperation(Op replacement) { return new Node(id, replacement, type, index, ref, literal, inputs.toArray(new String[0])); }
    }

    public static final class Module {
        public final String name, output;
        public final Phase phase;
        private final Type[] parameters;
        private final List<Node> nodes;
        public Module(String name, Phase phase, Type[] parameters, List<Node> nodes, String output) {
            identifier(name); identifier(output); this.name = name; this.phase = Objects.requireNonNull(phase);
            this.parameters = parameters.clone(); for (Type t : this.parameters) Objects.requireNonNull(t);
            this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes)); this.output = output;
        }
    }

    public static final class Update {
        public final int slot;
        public final String module;
        public Update(int slot, String module) { identifier(module); this.slot = slot; this.module = module; }
    }
    public static final class Limits {
        public final int maxModules, maxNodes, maxCallDepth, maxLag, maxStateSlots;
        public final long maxSteps;
        public Limits(int modules, int nodes, int depth, int lag, int slots, long steps) {
            require(modules >= 1 && modules <= 64 && nodes >= 1 && nodes <= 4096 && depth >= 1 && depth <= 16
                    && lag >= 1 && lag <= 4096 && slots >= 0 && slots <= 64 && steps >= 1 && steps <= 1000000, "LIMITS");
            maxModules = modules; maxNodes = nodes; maxCallDepth = depth; maxLag = lag; maxStateSlots = slots; maxSteps = steps;
        }
        public static Limits research() { return new Limits(32, 512, 8, 500, 16, 200000); }
        private String canonical() { return maxModules+":"+maxNodes+":"+maxCallDepth+":"+maxLag+":"+maxStateSlots+":"+maxSteps; }
    }

    public static final class HistoryRow {
        public final long issueOrdinal, availableAtMs;
        private final int[] numbers;
        public HistoryRow(long ordinal, long availableAtMs, int... numbers) {
            require(ordinal >= 0 && availableAtMs > 0, "HISTORY_IDENTITY"); permutation(numbers);
            issueOrdinal = ordinal; this.availableAtMs = availableAtMs; this.numbers = numbers.clone();
        }
    }
    public static final class Frame {
        public final long targetOrdinal, frozenAtMs;
        private final HistoryRow[] newestFirst;
        public Frame(long targetOrdinal, long frozenAtMs, HistoryRow... newestFirst) {
            require(targetOrdinal > 0 && frozenAtMs > 0 && newestFirst.length >= 1 && newestFirst.length <= 4096, "FRAME");
            this.targetOrdinal = targetOrdinal; this.frozenAtMs = frozenAtMs; this.newestFirst = newestFirst.clone();
            for (int i = 0; i < newestFirst.length; i++) {
                HistoryRow row = Objects.requireNonNull(newestFirst[i]);
                require(row.issueOrdinal == targetOrdinal-i-1 && row.availableAtMs <= frozenAtMs, "PIT_HISTORY");
            }
        }
        private HistoryRow lag(int lag) { require(lag >= 1 && lag <= newestFirst.length, "HISTORY_UNAVAILABLE"); return newestFirst[lag-1]; }
    }
    public static final class Settlement {
        public final long issueOrdinal, availableAtMs;
        private final int[] numbers;
        public Settlement(long ordinal, long at, int... numbers) {
            require(ordinal > 0 && at > 0, "SETTLEMENT_IDENTITY"); permutation(numbers);
            issueOrdinal = ordinal; availableAtMs = at; this.numbers = numbers.clone();
        }
    }
    public static final class State {
        public final String programCodeHash;
        public final long settledThrough, version;
        private final double[] values;
        private State(String identity, long through, long version, double[] values) {
            programCodeHash = identity; settledThrough = through; this.version = version; this.values = values.clone();
            for (double x : values) require(Double.isFinite(x), "STATE_NONFINITE");
        }
        public double value(int slot) { require(slot >= 0 && slot < values.length, "STATE_SLOT"); return values[slot]; }
    }
    public static final class Decision {
        public final int position, mask;
        public final double modelHitProbability;
        private Decision(int position, int mask, double probability) { this.position=position; this.mask=mask; modelHitProbability=probability; }
    }

    private final Map<String,Module> modules;
    private final Map<String,Long> costs = new HashMap<>();
    private final Map<String,String> functionHashes = new HashMap<>();
    private final Map<String,Integer> depths = new HashMap<>();
    private final String entry;
    private final List<Update> updates;
    private final int stateSlots;
    private final Limits limits;
    public final String codeHash, semanticHash;

    public TypedProgram(List<Module> definitions, String entry, int stateSlots, List<Update> updates, Limits limits) {
        identifier(entry); this.entry = entry; this.limits = Objects.requireNonNull(limits); this.stateSlots = stateSlots;
        require(stateSlots >= 0 && stateSlots <= limits.maxStateSlots && definitions.size() <= limits.maxModules, "PROGRAM_SIZE");
        TreeMap<String,Module> copy = new TreeMap<>(); int total = 0;
        for (Module m : definitions) { require(copy.put(m.name,m)==null, "DUPLICATE_MODULE"); total += m.nodes.size(); }
        require(total <= limits.maxNodes, "NODE_BUDGET"); modules = Collections.unmodifiableMap(copy);
        this.updates = Collections.unmodifiableList(new ArrayList<>(updates));
        for (String name : modules.keySet()) validateModule(name, new HashSet<String>());
        Module main = module(entry);
        require(main.phase == Phase.PREDICT && Arrays.equals(main.parameters,new Type[]{Type.POSITION})
                && outputType(main)==Type.PROB10, "ENTRY_CONTRACT");
        require(multiply(costs.get(entry),10) <= limits.maxSteps, "PREDICTION_STEP_BUDGET");
        Set<Integer> assigned = new HashSet<>(); long updateCost = 0;
        for (Update u : updates) {
            Module m = module(u.module); require(u.slot >= 0 && u.slot < stateSlots && assigned.add(u.slot), "UPDATE_SLOT");
            require(m.phase == Phase.SETTLE && m.parameters.length == 0 && outputType(m)==Type.SCALAR, "UPDATE_CONTRACT");
            updateCost = add(updateCost,costs.get(u.module));
        }
        require(updateCost <= limits.maxSteps, "UPDATE_STEP_BUDGET");
        StringBuilder source = new StringBuilder("SU1_REFERENCE_1|").append(limits.canonical()).append('|').append(stateSlots).append('|').append(entry);
        for (Module m : modules.values()) {
            source.append('|').append(m.name).append(':').append(m.phase).append(':').append(Arrays.toString(m.parameters)).append(':').append(m.output);
            for (Node n : m.nodes) source.append('|').append(n.id).append(':').append(n.op).append(':').append(n.type).append(':').append(n.index)
                    .append(':').append(n.ref).append(':').append(n.inputs).append(':').append(n.literal==null?"":n.literal.canonical());
        }
        StringBuilder behavior = new StringBuilder("SU1_SEMANTIC_REFERENCE_1|").append(stateSlots).append('|').append(functionHashes.get(entry));
        for (Update u : updates) { source.append("|U:").append(u.slot).append(':').append(u.module); behavior.append("|U:").append(u.slot).append(':').append(functionHashes.get(u.module)); }
        codeHash = hash(source.toString()); semanticHash = hash(behavior.toString());
    }

    private void validateModule(String name, Set<String> visiting) {
        if (costs.containsKey(name)) return;
        require(visiting.add(name), "MODULE_RECURSION"); Module m = module(name);
        require(!m.nodes.isEmpty(), "EMPTY_MODULE"); Map<String,Node> prior = new HashMap<>(); Map<String,String> expressions = new HashMap<>();
        long cost=0; int depth=1;
        for (Node n : m.nodes) {
            require(!prior.containsKey(n.id), "DUPLICATE_NODE"); List<Type> in = new ArrayList<>(); List<String> expr = new ArrayList<>();
            for (String p : n.inputs) { require(prior.containsKey(p), "DAG_PARENT_ORDER"); in.add(prior.get(p).type); expr.add(expressions.get(p)); }
            long extra=0;
            if (n.op==Op.CALL || n.op==Op.SET_WHERE || n.op==Op.SCORE_EACH_NUMBER) {
                Module callee=module(n.ref); validateModule(n.ref,visiting);
                require(m.phase==Phase.SETTLE || callee.phase==Phase.PREDICT, "POST_LABEL_IN_PREDICTION");
                depth=Math.max(depth,1+depths.get(n.ref)); extra=multiply(costs.get(n.ref),n.op==Op.CALL?1:10);
                expr.add(functionHashes.get(n.ref));
            }
            validateNode(m,n,in); cost=add(cost,add(1,extra)); require(cost<=limits.maxSteps,"MODULE_STEP_BUDGET");
            if (n.op==Op.SET_UNION || n.op==Op.SET_INTERSECTION || n.op==Op.ADD || n.op==Op.MULTIPLY || n.op==Op.SCORE_ADD || n.op==Op.EQ || n.op==Op.NE) Collections.sort(expr);
            expressions.put(n.id,hash(n.op+"|"+n.type+"|"+n.index+"|"+(n.literal==null?"":n.literal.canonical())+"|"+expr)); prior.put(n.id,n);
        }
        require(prior.containsKey(m.output),"MODULE_OUTPUT"); require(depth<=limits.maxCallDepth,"CALL_DEPTH");
        functionHashes.put(name,hash(m.phase+"|"+Arrays.toString(m.parameters)+"|"+expressions.get(m.output)));
        costs.put(name,cost); depths.put(name,depth); visiting.remove(name);
    }

    private void validateNode(Module m, Node n, List<Type> p) {
        switch(n.op) {
            case CONST: require(p.isEmpty() && n.literal!=null && n.literal.type==n.type,"CONST_TYPE"); return;
            case PARAM: require(p.isEmpty() && n.index>=0 && n.index<m.parameters.length && n.type==m.parameters[n.index],"PARAM_TYPE"); return;
            case STATE_READ: require(p.isEmpty() && n.type==Type.SCALAR && n.index>=0 && n.index<stateSlots,"STATE_READ_TYPE"); return;
            case READ_NUMBER: signature(n,p,Type.NUMBER,Type.POSITION); require(n.index>=1 && n.index<=limits.maxLag,"HISTORY_LAG"); return;
            case LOCATE_NUMBER: signature(n,p,Type.POSITION,Type.NUMBER); require(n.index>=1 && n.index<=limits.maxLag,"HISTORY_LAG"); return;
            case SETTLED_NUMBER: signature(n,p,Type.NUMBER,Type.POSITION); require(m.phase==Phase.SETTLE,"POST_LABEL_IN_PREDICTION"); return;
            case TO_SCALAR: require(n.type==Type.SCALAR && p.size()==1 && (p.get(0)==Type.NUMBER || p.get(0)==Type.POSITION),"ENTITY_CAST"); return;
            case EQ: case NE: require(n.type==Type.BOOL && p.size()==2 && p.get(0)==p.get(1) && p.get(0)!=Type.SCORE10 && p.get(0)!=Type.PROB10,"EQUALITY_TYPE"); return;
            case LESS: require(n.type==Type.BOOL && p.size()==2 && p.get(0)==p.get(1) && (p.get(0)==Type.SCALAR || p.get(0)==Type.NUMBER || p.get(0)==Type.POSITION),"ORDER_TYPE"); return;
            case ADD: case SUBTRACT: case MULTIPLY: signature(n,p,Type.SCALAR,Type.SCALAR,Type.SCALAR); return;
            case IF: require(p.size()==3 && p.get(0)==Type.BOOL && p.get(1)==p.get(2) && n.type==p.get(1),"IF_TYPE"); return;
            case SET_UNION: case SET_INTERSECTION: case SET_DIFFERENCE: signature(n,p,Type.SET10,Type.SET10,Type.SET10); return;
            case SET_COMPLEMENT: signature(n,p,Type.SET10,Type.SET10); return;
            case SET_CONTAINS: signature(n,p,Type.BOOL,Type.SET10,Type.NUMBER); return;
            case SET_COUNT: signature(n,p,Type.SCALAR,Type.SET10); return;
            case SINGLETON: signature(n,p,Type.SET10,Type.NUMBER); return;
            case SET_TO_SCORE: signature(n,p,Type.SCORE10,Type.SET10); return;
            case SCORE_ADD: signature(n,p,Type.SCORE10,Type.SCORE10,Type.SCORE10); return;
            case SCORE_SCALE: signature(n,p,Type.SCORE10,Type.SCORE10,Type.SCALAR); return;
            case PROB_TO_SCORE: signature(n,p,Type.SCORE10,Type.PROB10); return;
            case SOFTMAX: signature(n,p,Type.PROB10,Type.SCORE10); return;
            case PROB_MIX: signature(n,p,Type.PROB10,Type.PROB10,Type.PROB10,Type.SCALAR); return;
            case TOP3: require(n.type==Type.SET10 && p.size()==1 && (p.get(0)==Type.SCORE10 || p.get(0)==Type.PROB10),"TOP3_TYPE"); return;
            case CALL: case SET_WHERE: case SCORE_EACH_NUMBER:
                Module c=module(n.ref); int offset=n.op==Op.CALL?0:1;
                require(c.parameters.length==p.size()+offset,"CALL_ARITY");
                if(offset==1) require(c.parameters[0]==Type.NUMBER,"BIND_ENTITY_TYPE");
                for(int i=0;i<p.size();i++) require(p.get(i)==c.parameters[i+offset],"CALL_ARGUMENT_TYPE");
                if(n.op==Op.CALL) require(n.type==outputType(c),"CALL_RESULT_TYPE");
                else require(n.type==(n.op==Op.SET_WHERE?Type.SET10:Type.SCORE10) && outputType(c)==(n.op==Op.SET_WHERE?Type.BOOL:Type.SCALAR),"BIND_RESULT_TYPE");
                return;
            default: throw new IllegalArgumentException("UNREGISTERED_OP");
        }
    }

    public State initialState(long settledThrough) { require(settledThrough>=0,"STATE_WATERMARK"); return new State(codeHash,settledThrough,0,new double[stateSlots]); }
    private void stateFrame(State state,Frame frame) {
        require(state!=null && frame!=null && codeHash.equals(state.programCodeHash) && state.values.length==stateSlots,"STATE_PROGRAM_IDENTITY");
        require(state.settledThrough==frame.targetOrdinal-1,"STATE_TARGET_SEQUENCE");
    }
    public Value invoke(String name,Frame frame,State state,Value... arguments) {
        stateFrame(state,frame); require(module(name).phase==Phase.PREDICT,"POST_INVOKE_FORBIDDEN");
        return evaluate(name,arguments,new Eval(frame,state,null,limits.maxSteps),1);
    }
    public Decision predict(Frame frame,State state) {
        stateFrame(state,frame); Eval e=new Eval(frame,state,null,limits.maxSteps); Decision best=null;
        for(int position=1;position<=10;position++) {
            Value distribution=evaluate(entry,new Value[]{Value.position(position)},e,1); int mask=top3(distribution.vector);
            double probability=0; for(int d=0;d<10;d++) if((mask&(1<<d))!=0) probability+=distribution.vector[d];
            if(best==null || probability>best.modelHitProbability) best=new Decision(position,mask,probability);
        }
        return best;
    }
    public State settle(Frame frame,State old,Settlement settlement) {
        stateFrame(old,frame); require(settlement!=null && settlement.issueOrdinal==frame.targetOrdinal
                && settlement.availableAtMs>frame.frozenAtMs,"SETTLEMENT_PHASE");
        Eval e=new Eval(frame,old,settlement,limits.maxSteps); double[] next=old.values.clone();
        // Every RHS observes old state. Return a new state only after ALL updates succeed.
        for(Update u:updates) next[u.slot]=evaluate(u.module,new Value[0],e,1).scalar();
        return new State(codeHash,settlement.issueOrdinal,old.version+1,next);
    }
    public TypedProgram replaceOperation(String moduleName,String nodeId,Op replacement) {
        List<Module> next=new ArrayList<>(); boolean found=false;
        for(Module m:modules.values()) {
            List<Node> nodes=new ArrayList<>(); for(Node n:m.nodes) {
                if(m.name.equals(moduleName)&&n.id.equals(nodeId)) {nodes.add(n.withOperation(replacement));found=true;} else nodes.add(n);
            }
            next.add(new Module(m.name,m.phase,m.parameters,nodes,m.output));
        }
        require(found,"MUTATION_NODE_MISSING"); TypedProgram child=new TypedProgram(next,entry,stateSlots,updates,limits);
        require(!child.semanticHash.equals(semanticHash),"MUTATION_NO_SEMANTIC_CHANGE"); return child;
    }
    /** Compose two compatible PREDICT functions within this immutable dependency universe. */
    public TypedProgram withProbabilityFusion(String newEntry,String left,String right,double weight) {
        require(Double.isFinite(weight)&&weight>0&&weight<1,"FUSION_WEIGHT");
        require(!modules.containsKey(newEntry)&&!functionHashes.getOrDefault(left,"").equals(functionHashes.getOrDefault(right,"")),"FUSION_DUPLICATE");
        List<Module> next=new ArrayList<>(modules.values());
        next.add(new Module(newEntry,Phase.PREDICT,new Type[]{Type.POSITION},Arrays.asList(
                Node.parameter("p",Type.POSITION,0),Node.call("a",left,Type.PROB10,"p"),Node.call("b",right,Type.PROB10,"p"),
                Node.constant("w",Value.scalar(weight)),Node.of("mix",Op.PROB_MIX,Type.PROB10,"a","b","w")),"mix"));
        return new TypedProgram(next,newEntry,stateSlots,updates,limits);
    }

    private static final class Eval {
        final Frame frame; final State state; final Settlement settlement; long remaining;
        Eval(Frame f,State s,Settlement label,long steps){frame=f;state=s;settlement=label;remaining=steps;}
        void tick(){require(--remaining>=0,"EXECUTION_STEP_BUDGET");}
    }
    private Value evaluate(String name,Value[] args,Eval e,int depth) {
        require(depth<=limits.maxCallDepth,"CALL_DEPTH"); Module m=module(name);
        require(args.length==m.parameters.length,"RUNTIME_CALL_ARITY");
        for(int i=0;i<args.length;i++)require(args[i]!=null&&args[i].type==m.parameters[i],"RUNTIME_ARGUMENT_TYPE");
        require(m.phase==Phase.PREDICT||e.settlement!=null,"POST_LABEL_UNAVAILABLE"); Map<String,Value> values=new HashMap<>();
        for(Node n:m.nodes) { e.tick(); Value[] p=new Value[n.inputs.size()];for(int i=0;i<p.length;i++)p[i]=values.get(n.inputs.get(i));
            Value out;
            switch(n.op) {
                case CONST: out=n.literal;break;
                case PARAM: out=args[n.index];break;
                case STATE_READ: out=Value.scalar(e.state.value(n.index));break;
                case READ_NUMBER: out=Value.number(e.frame.lag(n.index).numbers[p[0].integer()-1]);break;
                case LOCATE_NUMBER: {int[] row=e.frame.lag(n.index).numbers;int pos=0;while(row[pos]!=p[0].integer())pos++;out=Value.position(pos+1);break;}
                case SETTLED_NUMBER: require(e.settlement!=null,"POST_LABEL_UNAVAILABLE");out=Value.number(e.settlement.numbers[p[0].integer()-1]);break;
                case TO_SCALAR: out=Value.scalar(p[0].scalar());break;
                case EQ: out=Value.bool(p[0].scalar()==p[1].scalar());break;
                case NE: out=Value.bool(p[0].scalar()!=p[1].scalar());break;
                case LESS: out=Value.bool(p[0].scalar()<p[1].scalar());break;
                case ADD: out=Value.scalar(p[0].scalar()+p[1].scalar());break;
                case SUBTRACT: out=Value.scalar(p[0].scalar()-p[1].scalar());break;
                case MULTIPLY: out=Value.scalar(p[0].scalar()*p[1].scalar());break;
                case IF: out=p[p[0].bool()?1:2];break;
                case SET_UNION: out=Value.set(p[0].integer()|p[1].integer());break;
                case SET_INTERSECTION: out=Value.set(p[0].integer()&p[1].integer());break;
                case SET_DIFFERENCE: out=Value.set(p[0].integer()&(~p[1].integer()&1023));break;
                case SET_COMPLEMENT: out=Value.set(~p[0].integer()&1023);break;
                case SET_CONTAINS: out=Value.bool((p[0].integer()&(1<<(p[1].integer()-1)))!=0);break;
                case SET_COUNT: out=Value.scalar(Integer.bitCount(p[0].integer()));break;
                case SINGLETON: out=Value.set(1<<(p[0].integer()-1));break;
                case SET_TO_SCORE: {double[] x=new double[10];for(int i=0;i<10;i++)x[i]=(p[0].integer()&(1<<i))==0?0:1;out=Value.score(x);break;}
                case SCORE_ADD: {double[] x=p[0].vector();for(int i=0;i<10;i++)x[i]+=p[1].vector[i];out=Value.score(x);break;}
                case SCORE_SCALE: {double[] x=p[0].vector();for(int i=0;i<10;i++)x[i]*=p[1].scalar();out=Value.score(x);break;}
                case PROB_TO_SCORE: out=Value.score(p[0].vector);break;
                case SOFTMAX: {double[] x=p[0].vector();double max=x[0],sum=0;for(double v:x)max=Math.max(max,v);for(int i=0;i<10;i++){x[i]=Math.exp(x[i]-max);sum+=x[i];}for(int i=0;i<10;i++)x[i]/=sum;out=Value.probability(x);break;}
                case PROB_MIX: {double w=p[2].scalar();require(w>=0&&w<=1,"FUSION_WEIGHT");double[] x=new double[10];for(int i=0;i<10;i++)x[i]=w*p[0].vector[i]+(1-w)*p[1].vector[i];out=Value.probability(x);break;}
                case TOP3: out=Value.set(top3(p[0].vector));break;
                case CALL: out=evaluate(n.ref,p,e,depth+1);break;
                case SET_WHERE: case SCORE_EACH_NUMBER: {
                    int mask=0;double[] x=new double[10];Value[] bound=new Value[p.length+1];System.arraycopy(p,0,bound,1,p.length);
                    for(int number=1;number<=10;number++){bound[0]=Value.number(number);Value value=evaluate(n.ref,bound,e,depth+1);if(n.op==Op.SET_WHERE){if(value.bool())mask|=1<<(number-1);}else x[number-1]=value.scalar();}
                    out=n.op==Op.SET_WHERE?Value.set(mask):Value.score(x);break;
                }
                default: throw new IllegalArgumentException("UNREGISTERED_OP");
            }
            require(out.type==n.type,"RUNTIME_RESULT_TYPE");values.put(n.id,out);
        }
        return values.get(m.output);
    }
    private Module module(String name) { Module m=modules.get(name);require(m!=null,"MODULE_MISSING:"+name);return m; }
    private static Type outputType(Module m) { for(Node n:m.nodes)if(n.id.equals(m.output))return n.type;throw new IllegalArgumentException("MODULE_OUTPUT"); }
    private static void signature(Node n,List<Type> parents,Type output,Type... inputs) { require(n.type==output&&parents.equals(Arrays.asList(inputs)),"TYPE:"+n.op); }
    private static int top3(double[] x) { Integer[] order={0,1,2,3,4,5,6,7,8,9};Arrays.sort(order,(a,b)->{int c=Double.compare(x[b],x[a]);return c==0?Integer.compare(a,b):c;});return (1<<order[0])|(1<<order[1])|(1<<order[2]); }
    private static void permutation(int[] row) { require(row!=null&&row.length==10,"PERMUTATION_SIZE");int seen=0;for(int n:row){require(n>=1&&n<=10&&(seen&(1<<(n-1)))==0,"PERMUTATION_VALUE");seen|=1<<(n-1);} }
    private static void identifier(String s) { require(s!=null&&s.matches("[A-Za-z][A-Za-z0-9_]{0,63}"),"IDENTIFIER"); }
    private static long add(long a,long b) { require(a>=0&&b>=0&&a<=Long.MAX_VALUE-b,"COST_OVERFLOW");return a+b; }
    private static long multiply(long a,long b) { require(a>=0&&b>=0&&(b==0||a<=Long.MAX_VALUE/b),"COST_OVERFLOW");return a*b; }
    private static void require(boolean ok,String code) { if(!ok)throw new IllegalArgumentException(code); }
    private static String hash(String value) { try {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(Character.forDigit((x>>>4)&15,16)).append(Character.forDigit(x&15,16));return b.toString();} catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);} }
}
