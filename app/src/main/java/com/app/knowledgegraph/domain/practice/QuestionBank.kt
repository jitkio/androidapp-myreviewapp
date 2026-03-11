package com.app.knowledgegraph.domain.practice

object QuestionBank {

    private val allMethods = listOf(
        "叠加定理", "戴维南定理", "诺顿定理", "源变换",
        "节点电压法", "网孔电流法", "最大功率传输"
    )

    fun getMethodOptions(correctMethod: String): List<String> {
        val options = allMethods.toMutableList()
        if (correctMethod !in options) options.add(correctMethod)
        return options.shuffled()
    }

    fun fetchAllQuestions(): List<PracticeQuestion> = chapter4Questions

    fun getQuestionsByChapter(chapter: String): List<PracticeQuestion> =
        allQuestions.filter { it.chapter == chapter }

    private val allQuestions: List<PracticeQuestion> get() = chapter4Questions

    private val chapter4Questions = listOf(
        PracticeQuestion(
            id = "c4_01",
            chapter = "第4章-电路定理",
            stem = "线性电路含3个独立电压源，求某一条支路电流。电路为平面电路，无受控源。",
            methods = allMethods,
            correctMethod = "叠加定理",
            triggerWords = mapOf(
                "线性" to "叠加定理要求线性电路",
                "3个独立电压源" to "多个独立源，适合逐个分析",
                "某一条支路" to "只求一个响应，叠加法高效"
            ),
            conditions = listOf(
                ConditionCheck("电路是否线性？", true, "是，可以用叠加"),
                ConditionCheck("有受控源？", false, "无受控源，叠加时不需要特殊处理"),
                ConditionCheck("求单个响应？", true, "只求一条支路，叠加合适"),
                ConditionCheck("独立源多个？", true, "3个独立源，叠加可分解")
            ),
            explanation = "3个独立源、线性、只求一条支路电流，叠加定理可以将复杂问题分解为3个简单子电路分别求解。",
            commonTraps = "注意：受控源如果有的话必须保留在每个子电路中"
        ),
        PracticeQuestion(
            id = "c4_02",
            chapter = "第4章-电路定理",
            stem = "含独立源的线性二端网络，负载RL可变，求RL取何值时获得最大功率。",
            methods = allMethods,
            correctMethod = "最大功率传输",
            triggerWords = mapOf(
                "负载RL可变" to "负载变化分析，先求等效电路",
                "最大功率" to "直接指向最大功率传输定理",
                "线性二端网络" to "可用戴维南等效"
            ),
            conditions = listOf(
                ConditionCheck("电路是否线性？", true, "是"),
                ConditionCheck("负载可变？", true, "需分析负载变化的影响"),
                ConditionCheck("求最大功率？", true, "RL=Req时Pmax")
            ),
            explanation = "先用戴维南定理求Uoc和Req，再由最大功率条件RL=Req得到答案，Pmax=Uoc^2/(4*Req)。",
            commonTraps = "先求戴维南等效，再套最大功率公式，不要跳步"
        ),
        PracticeQuestion(
            id = "c4_03",
            chapter = "第4章-电路定理",
            stem = "求下图电路中R3支路的电流。电路含1个电压源和1个电流源，共4个节点6条支路，平面电路。",
            methods = allMethods,
            correctMethod = "戴维南定理",
            triggerWords = mapOf(
                "R3支路的电流" to "只求一条支路，优先考虑戴维南",
                "1个电压源和1个电流源" to "源不多，戴维南直接求"
            ),
            conditions = listOf(
                ConditionCheck("只求一条支路？", true, "戴维南最高效"),
                ConditionCheck("电路是否线性？", true, "可以用戴维南"),
                ConditionCheck("有受控源？", false, "Req可以直接置零法求")
            ),
            explanation = "只求R3的电流，断开R3后求开路电压Uoc和等效电阻Req，再由I=Uoc/(Req+R3)即可。比节点法或网孔法少解很多方程。",
            commonTraps = "断开R3时注意标好电压极性"
        ),
        PracticeQuestion(
            id = "c4_04",
            chapter = "第4章-电路定理",
            stem = "电路有5个节点、8条支路，平面电路，含2个电流源，需求所有支路电流。",
            methods = allMethods,
            correctMethod = "节点电压法",
            triggerWords = mapOf(
                "5个节点" to "节点法需4个方程",
                "8条支路" to "网孔法需8-5+1=4个方程，和节点法一样",
                "所有支路电流" to "需要系统方法求全部",
                "2个电流源" to "电流源在节点法中直接代入，更方便"
            ),
            conditions = listOf(
                ConditionCheck("求所有支路？", true, "需要系统方法"),
                ConditionCheck("含电流源多？", true, "2个电流源，节点法方便"),
                ConditionCheck("节点法方程数？", true, "n-1=4"),
                ConditionCheck("网孔法方程数？", true, "b-n+1=4")
            ),
            explanation = "方程数相同但含2个电流源，节点法中电流源直接给出节点电流关系，列方程更方便。",
            commonTraps = "如果电流源接在两个独立节点之间，要用超节点法"
        ),
        PracticeQuestion(
            id = "c4_05",
            chapter = "第4章-电路定理",
            stem = "梯形电路，含3个电压源，需求所有网孔电流。电路有3个网孔、5个节点。",
            methods = allMethods,
            correctMethod = "网孔电流法",
            triggerWords = mapOf(
                "梯形电路" to "梯形电路用网孔法特别方便",
                "3个电压源" to "电压源在网孔法中直接代入",
                "3个网孔" to "网孔法只需3个方程",
                "所有网孔电流" to "网孔法直接求的就是网孔电流"
            ),
            conditions = listOf(
                ConditionCheck("平面电路？", true, "可以用网孔法"),
                ConditionCheck("含电压源多？", true, "3个电压源，网孔法方便"),
                ConditionCheck("梯形电路？", true, "网孔法特别适合"),
                ConditionCheck("网孔法方程数？", true, "3个方程")
            ),
            explanation = "梯形电路+电压源多=网孔法最佳。3个方程即可求解。",
            commonTraps = "注意统一网孔电流方向（通常全部顺时针）"
        ),
        PracticeQuestion(
            id = "c4_06",
            chapter = "第4章-电路定理",
            stem = "非平面电路，含受控源和2个独立源，需求某节点电压。共3个独立节点。",
            methods = allMethods,
            correctMethod = "节点电压法",
            triggerWords = mapOf(
                "非平面电路" to "非平面不能用网孔法！",
                "含受控源" to "受控源在节点法中用控制量表示",
                "3个独立节点" to "只需3个方程"
            ),
            conditions = listOf(
                ConditionCheck("平面电路？", false, "非平面，网孔法不能用！"),
                ConditionCheck("有受控源？", true, "需要用控制量表示"),
                ConditionCheck("节点法可用？", true, "不受平面限制")
            ),
            explanation = "非平面电路排除网孔法，只能用节点电压法。受控源用节点电压表示控制变量后代入方程。",
            commonTraps = "受控源的控制变量必须用节点电压表示"
        ),
        PracticeQuestion(
            id = "c4_07",
            chapter = "第4章-电路定理",
            stem = "线性电路中，已知某支路电流为I，现将电路中某个电压源的值从10V变为15V，其他不变，求该支路新电流。",
            methods = allMethods,
            correctMethod = "叠加定理",
            triggerWords = mapOf(
                "电压源的值从10V变为15V" to "源值变化，可用叠加分析增量",
                "线性电路" to "满足叠加条件",
                "其他不变" to "只有一个源变化"
            ),
            conditions = listOf(
                ConditionCheck("线性电路？", true, "可叠加"),
                ConditionCheck("只改变一个源？", true, "增量分析用叠加"),
                ConditionCheck("求变化后的响应？", true, "新响应=原响应+增量源单独响应")
            ),
            explanation = "利用叠加原理：新电流 = 原电流 + (15V-10V)=5V单独作用时的响应。不需要重新求解整个电路。",
            commonTraps = "增量源是5V而不是15V"
        ),
        PracticeQuestion(
            id = "c4_08",
            chapter = "第4章-电路定理",
            stem = "含受控源的线性电路，求端口ab的戴维南等效电路。",
            methods = allMethods,
            correctMethod = "戴维南定理",
            triggerWords = mapOf(
                "戴维南等效" to "直接要求戴维南",
                "含受控源" to "求Req时要用外加电源法",
                "端口ab" to "二端网络等效"
            ),
            conditions = listOf(
                ConditionCheck("有受控源？", true, "Req不能直接串并联化简"),
                ConditionCheck("用什么求Req？", true, "外加电源法：Req=Ut/It")
            ),
            explanation = "先断开端口求Uoc，再用外加电源法求Req（独立源置零，受控源保留，外加Ut求It）。",
            commonTraps = "受控源必须保留！不能和独立源一起置零"
        ),
        PracticeQuestion(
            id = "c4_09",
            chapter = "第4章-电路定理",
            stem = "电路含1个电压源串联2个电阻，后接并联的R3和R4。求R4上的电流。",
            methods = allMethods,
            correctMethod = "源变换",
            triggerWords = mapOf(
                "电压源串联2个电阻" to "串联结构可以源变换",
                "并联的R3和R4" to "变换后变成并联结构，容易求解"
            ),
            conditions = listOf(
                ConditionCheck("简单串并联？", true, "结构简单，源变换即可"),
                ConditionCheck("需要系统方法？", false, "不需要列方程组")
            ),
            explanation = "将电压源串联电阻变换为电流源并联电阻，然后合并并联电阻，直接用分流公式。",
            commonTraps = "变换时注意电流源方向与电压源极性的对应关系"
        ),
        PracticeQuestion(
            id = "c4_10",
            chapter = "第4章-电路定理",
            stem = "线性电路，负载RL从端口看入的等效电路已知Uoc=10V、Req=5欧，现RL=15欧，求RL上的电流和功率。",
            methods = allMethods,
            correctMethod = "戴维南定理",
            triggerWords = mapOf(
                "等效电路已知" to "已有戴维南参数，直接套公式",
                "Uoc=10V" to "开路电压已知",
                "Req=5欧" to "等效电阻已知"
            ),
            conditions = listOf(
                ConditionCheck("戴维南参数已知？", true, "直接代公式"),
                ConditionCheck("求负载电流和功率？", true, "I=Uoc/(Req+RL)")
            ),
            explanation = "I=Uoc/(Req+RL)=10/(5+15)=0.5A, P=I^2*RL=0.5^2*15=3.75W",
            commonTraps = "别忘了功率用的是RL不是Req"
        ),
        PracticeQuestion(
            id = "c4_11",
            chapter = "第4章-电路定理",
            stem = "含2个电流源并联，后串联一个电阻接地，求电阻上的电压。",
            methods = allMethods,
            correctMethod = "源变换",
            triggerWords = mapOf(
                "2个电流源并联" to "电流源并联可直接合并",
                "串联一个电阻" to "合并后乘以电阻即可"
            ),
            conditions = listOf(
                ConditionCheck("简单结构？", true, "并联电流源直接合并"),
                ConditionCheck("需要系统方法？", false, "源合并+欧姆定律")
            ),
            explanation = "并联电流源合并：Is=Is1+Is2（注意方向），U=Is*R。",
            commonTraps = "注意两个电流源的方向，同向相加，反向相减"
        ),
        PracticeQuestion(
            id = "c4_12",
            chapter = "第4章-电路定理",
            stem = "桥式电路，5个电阻加1个电压源，求桥臂上某电阻的电流。",
            methods = allMethods,
            correctMethod = "戴维南定理",
            triggerWords = mapOf(
                "桥式电路" to "桥式不能简单串并联，但可以戴维南",
                "某电阻的电流" to "只求一条支路，戴维南高效"
            ),
            conditions = listOf(
                ConditionCheck("只求一条支路？", true, "戴维南最合适"),
                ConditionCheck("能直接串并联？", false, "桥式不能简单化简")
            ),
            explanation = "断开待求支路，利用其余部分求戴维南等效（Uoc和Req），再接回负载求电流。",
            commonTraps = "求Req时桥式需要注意哪些串联哪些并联"
        ),
        PracticeQuestion(
            id = "c4_13",
            chapter = "第4章-电路定理",
            stem = "负载并联结构，含3个独立电流源，需求负载电压。线性电路。",
            methods = allMethods,
            correctMethod = "诺顿定理",
            triggerWords = mapOf(
                "负载并联结构" to "并联结构用诺顿更方便",
                "电流源" to "诺顿本身就是电流源模型",
                "3个独立电流源" to "也可考虑叠加"
            ),
            conditions = listOf(
                ConditionCheck("并联负载？", true, "诺顿定理更自然"),
                ConditionCheck("线性？", true, "可用诺顿")
            ),
            explanation = "负载是并联接入的，用诺顿定理（电流源Isc并联Req）等效后，直接算分流。也可用叠加。",
            commonTraps = "诺顿和戴维南结果一样，只是并联结构时诺顿计算更方便"
        ),
        PracticeQuestion(
            id = "c4_14",
            chapter = "第4章-电路定理",
            stem = "电路含1个受控电压源（受控于某支路电流），2个独立电压源。求所有支路电流。4个节点，6条支路，平面电路。",
            methods = allMethods,
            correctMethod = "网孔电流法",
            triggerWords = mapOf(
                "所有支路电流" to "需要系统方法",
                "2个独立电压源" to "网孔法中电压源直接代入",
                "平面电路" to "可以用网孔法",
                "受控电压源" to "受控源用网孔电流表示"
            ),
            conditions = listOf(
                ConditionCheck("求所有支路？", true, "系统方法"),
                ConditionCheck("平面电路？", true, "可用网孔法"),
                ConditionCheck("电压源多？", true, "网孔法有优势"),
                ConditionCheck("网孔数？", true, "b-n+1=6-4+1=3个方程")
            ),
            explanation = "平面电路+电压源为主+求全部支路=网孔电流法。受控源的控制变量用网孔电流表示。",
            commonTraps = "受控源不能置零，必须用网孔电流表示后代入方程"
        ),
        PracticeQuestion(
            id = "c4_15",
            chapter = "第4章-电路定理",
            stem = "线性电路含多个电阻和2个独立源，求某一条支路的电压和电流。已知如果把该支路断开，端口开路电压容易求得。",
            methods = allMethods,
            correctMethod = "戴维南定理",
            triggerWords = mapOf(
                "某一条支路" to "单支路优先戴维南",
                "端口开路电压容易求得" to "强烈暗示用戴维南",
                "把该支路断开" to "这就是戴维南的第一步"
            ),
            conditions = listOf(
                ConditionCheck("只求一条支路？", true, "戴维南"),
                ConditionCheck("Uoc容易求？", true, "戴维南计算量小")
            ),
            explanation = "题目暗示了戴维南的思路：断开支路求Uoc，再求Req，最后I=Uoc/(Req+R)。",
            commonTraps = "做题时要养成判断Uoc是否容易求的习惯"
        ),
        PracticeQuestion(
            id = "c4_16",
            chapter = "第4章-电路定理",
            stem = "运算放大器电路（理想），含受控源模型，求输出电压与输入电压的关系。2个独立节点。",
            methods = allMethods,
            correctMethod = "节点电压法",
            triggerWords = mapOf(
                "运算放大器" to "运放分析常用节点法",
                "2个独立节点" to "节点法只需2个方程",
                "输出电压与输入电压的关系" to "节点电压直接就是要求的量"
            ),
            conditions = listOf(
                ConditionCheck("含受控源？", true, "运放本质是受控源"),
                ConditionCheck("节点少？", true, "2个节点，方程少"),
                ConditionCheck("求电压关系？", true, "节点电压就是所求")
            ),
            explanation = "运放电路中，利用虚短虚断条件+节点电压法，2个方程直接求解。",
            commonTraps = "虚短：U+=U-；虚断：I+=I-=0"
        ),
        PracticeQuestion(
            id = "c4_17",
            chapter = "第4章-电路定理",
            stem = "线性电路含4个独立电压源和2个独立电流源，只求其中一个电流源两端的电压。",
            methods = allMethods,
            correctMethod = "叠加定理",
            triggerWords = mapOf(
                "4个独立电压源和2个独立电流源" to "大量独立源，叠加分解",
                "只求其中一个" to "只求一个响应",
                "线性电路" to "满足叠加条件"
            ),
            conditions = listOf(
                ConditionCheck("线性？", true, "可叠加"),
                ConditionCheck("独立源很多？", true, "6个独立源"),
                ConditionCheck("只求一个响应？", true, "叠加分解后每个子电路很简单")
            ),
            explanation = "6个独立源虽多，但叠加后每个子电路只含1个源，大大简化。只求一个响应时叠加很高效。",
            commonTraps = "6个子电路虽然多，但每个都很简单"
        ),
        PracticeQuestion(
            id = "c4_18",
            chapter = "第4章-电路定理",
            stem = "求电路的诺顿等效电路（短路电流Isc和等效电阻Req）。电路含受控源。",
            methods = allMethods,
            correctMethod = "诺顿定理",
            triggerWords = mapOf(
                "诺顿等效" to "直接要求诺顿",
                "短路电流Isc" to "诺顿参数",
                "含受控源" to "Req用外加电源法"
            ),
            conditions = listOf(
                ConditionCheck("求诺顿等效？", true, "直接用诺顿定理"),
                ConditionCheck("有受控源？", true, "外加电源法求Req")
            ),
            explanation = "短路端口求Isc，再用外加电源法求Req。或者先求戴维南再转换。",
            commonTraps = "也可以先求Uoc和Req，再由Isc=Uoc/Req得到"
        ),
        PracticeQuestion(
            id = "c4_19",
            chapter = "第4章-电路定理",
            stem = "电路中某个电阻R的值改变，需分析R变化对电路某支路电流的影响。线性电路。",
            methods = allMethods,
            correctMethod = "戴维南定理",
            triggerWords = mapOf(
                "电阻R的值改变" to "R变化=负载变化，戴维南最合适",
                "分析R变化对...的影响" to "戴维南等效后R就是负载"
            ),
            conditions = listOf(
                ConditionCheck("参数变化分析？", true, "戴维南：等效后只改RL"),
                ConditionCheck("线性？", true, "可用戴维南")
            ),
            explanation = "把R看作负载，其余电路求戴维南等效。R变化时只需重新算I=Uoc/(Req+R)，不用重新分析整个电路。",
            commonTraps = "这就是戴维南定理的核心优势：分析负载变化"
        ),
        PracticeQuestion(
            id = "c4_20",
            chapter = "第4章-电路定理",
            stem = "非线性电路（含二极管），求二极管的工作点电流。电路其余部分是线性的。",
            methods = allMethods,
            correctMethod = "戴维南定理",
            triggerWords = mapOf(
                "非线性" to "不能用叠加定理！",
                "二极管" to "非线性元件",
                "其余部分是线性的" to "线性部分可以戴维南等效"
            ),
            conditions = listOf(
                ConditionCheck("整体线性？", false, "含非线性元件"),
                ConditionCheck("能用叠加？", false, "非线性不能叠加！"),
                ConditionCheck("线性部分可等效？", true, "戴维南等效后与二极管联立")
            ),
            explanation = "将二极管以外的线性部分用戴维南等效，得到Uoc和Req，然后与二极管特性联立求解工作点。",
            commonTraps = "绝对不能用叠加定理！非线性电路只能用戴维南/诺顿等效线性部分"
        )
    )
}
