package com.app.knowledgegraph.data.db

import com.app.knowledgegraph.data.db.entity.*
import com.app.knowledgegraph.data.repository.CardRepository
import com.app.knowledgegraph.data.repository.GraphRepository
import com.app.knowledgegraph.data.repository.QuestionBankRepository

object DatabaseSeeder {

    suspend fun seedIfEmpty(
        cardRepository: CardRepository,
        graphRepository: GraphRepository,
        questionBankRepository: QuestionBankRepository
    ) {
        // 只要数据库中已有任意卡片，就不再插入默认数据
        if (cardRepository.getCount() > 0) return

        val cardIds = mutableListOf<Long>()

        cardIds += cardRepository.createCard(Card(
            type = CardType.CONCEPT,
            chapter = "第4章-电路定理",
            tags = "叠加定理,线性,独立源",
            prompt = "叠加定理的适用条件和核心思想是什么？",
            hint = "想想【线性】这个关键词",
            answer = "【适用条件】\n" +
                "- 电路必须是线性电路\n" +
                "- 只对独立源进行叠加\n" +
                "- 受控源不能单独作用（保留在每个子电路中）\n\n" +
                "【核心思想】\n" +
                "线性电路中，任一支路的响应等于各个独立源单独作用时产生的响应之和。\n\n" +
                "【单独作用的含义】\n" +
                "- 电压源置零 = 短路\n" +
                "- 电流源置零 = 开路\n\n" +
                "【注意】功率不能直接叠加！"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.CONCEPT,
            chapter = "第4章-电路定理",
            tags = "戴维南,等效电路,开路电压,等效电阻",
            prompt = "戴维南定理的内容？如何求等效电路的两个参数？",
            hint = "一个电压源串联一个电阻",
            answer = "【定理内容】\n" +
                "含独立源的线性二端网络可等效为 Uoc 串联 Req。\n\n" +
                "【求 Uoc】断开负载，求端口开路电压。\n\n" +
                "【求 Req 三种方法】\n" +
                "1. 独立源置零法\n" +
                "2. 开路短路法：Req = Uoc / Isc\n" +
                "3. 外加电源法（含受控源时）"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.CONCEPT,
            chapter = "第4章-电路定理",
            tags = "诺顿,等效电路,短路电流",
            prompt = "诺顿定理与戴维南定理的关系？",
            hint = "对偶关系：电压源串联 vs 电流源并联",
            answer = "【定理内容】\n" +
                "含独立源的线性二端网络可等效为 Isc 并联 Req。\n\n" +
                "【与戴维南的关系】\n" +
                "- 互为对偶形式\n" +
                "- Isc = Uoc / Req\n" +
                "- 可通过源变换互相转换"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.CONCEPT,
            chapter = "第4章-电路定理",
            tags = "最大功率,阻抗匹配,戴维南",
            prompt = "负载获得最大功率的条件？最大功率是多少？",
            hint = "与戴维南等效电阻的关系",
            answer = "【条件】RL = Req 时负载获最大功率。\n\n" +
                "【最大功率】Pmax = Uoc^2 / (4*Req)\n\n" +
                "【注意】此时效率只有50%"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.CONCEPT,
            chapter = "第4章-电路定理",
            tags = "源变换,电压源,电流源,等效",
            prompt = "电压源和电流源如何互相变换？有什么限制？",
            hint = "串联变并联",
            answer = "【变换规则】\n" +
                "电压源Us串联Rs 可变换为 电流源Is并联Rs\n" +
                "Is = Us / Rs\n\n" +
                "【限制】\n" +
                "- 理想电压源不能变换为电流源\n" +
                "- 理想电流源不能变换为电压源\n" +
                "- 只保证对外等效"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.METHOD,
            chapter = "第4章-电路定理",
            tags = "节点法,KCL,系统方法",
            prompt = "节点电压法的步骤和适用场景？",
            hint = "选参考点 - 列KCL - 解方程",
            answer = "【步骤】\n" +
                "1. 选参考节点\n" +
                "2. 标记独立节点电压\n" +
                "3. 对每个独立节点列KCL方程\n" +
                "4. 解方程组\n\n" +
                "【适用】节点少支路多；含电流源多\n\n" +
                "【方程数 = n-1】"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.METHOD,
            chapter = "第4章-电路定理",
            tags = "网孔法,KVL,系统方法,平面电路",
            prompt = "网孔电流法的步骤和适用场景？",
            hint = "只适用于平面电路",
            answer = "【步骤】\n" +
                "1. 确认是平面电路\n" +
                "2. 指定网孔电流（顺时针）\n" +
                "3. 对每个网孔列KVL方程\n" +
                "4. 解方程组\n\n" +
                "【适用】网孔少节点多；含电压源多\n" +
                "【限制】非平面电路不能用！\n\n" +
                "【方程数 = b-n+1】"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.METHOD,
            chapter = "第4章-电路定理",
            tags = "戴维南,求解步骤,等效电路",
            prompt = "用戴维南定理求某支路电流的完整步骤？",
            hint = "断开 - 求Uoc - 求Req - 重新连接",
            answer = "第1步：断开待求支路\n\n" +
                "第2步：求开路电压Uoc\n\n" +
                "第3步：求等效电阻Req\n" +
                "- 无受控源：独立源置零看入\n" +
                "- 有受控源：外加电源法\n\n" +
                "第4步：I = Uoc / (Req + RL)\n\n" +
                "【检查】受控源保留了吗？极性标对了吗？"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.TEMPLATE,
            chapter = "第4章-电路定理",
            tags = "叠加定理,解题模板",
            prompt = "叠加法解题的标准模板？",
            hint = "分 - 求 - 合",
            answer = "第1步【分解】列出所有独立源\n\n" +
                "第2步【逐个求解】\n" +
                "- 保留一个独立源，其余置零\n" +
                "- 电压源置零=短路，电流源置零=开路\n" +
                "- 受控源始终保留\n\n" +
                "第3步【叠加】i = i1 + i2 + ...\n" +
                "注意参考方向和正负号\n\n" +
                "功率不能叠加！非线性不能用！"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.TEMPLATE,
            chapter = "第4章-电路定理",
            tags = "方法选择,速查表,决策树",
            prompt = "电路分析方法选择的决策树？",
            hint = "看电路特征选方法",
            answer = "求某一条支路 → 戴维南/诺顿\n\n" +
                "求所有支路：\n" +
                "  节点少 → 节点法\n" +
                "  网孔少 → 网孔法\n\n" +
                "多个独立源求一个响应 → 叠加\n\n" +
                "分析负载变化 → 戴维南\n\n" +
                "求最大功率 → 戴维南+最大功率定理\n\n" +
                "简单串并联 → 源变换+电阻化简"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.BOUNDARY,
            chapter = "第4章-电路定理",
            tags = "叠加定理,易错,功率,受控源",
            prompt = "使用叠加定理最常见的两个错误？",
            hint = "功率 和 受控源",
            answer = "【错误1：功率叠加】\n" +
                "功率不能叠加！P与i^2成正比，非线性。\n" +
                "正确：先叠加求总电流，再算功率。\n\n" +
                "【错误2：受控源也置零】\n" +
                "受控源必须在每个子电路中保留！\n" +
                "它不是独立激励。\n\n" +
                "【其他】参考方向正负号搞错；非线性电路误用"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.BOUNDARY,
            chapter = "第4章-电路定理",
            tags = "戴维南,受控源,外加电源法,易错",
            prompt = "含受控源时求Req，为什么不能直接用置零法？",
            hint = "受控源依赖于电路内部变量",
            answer = "【原因】受控源不是简单电阻，不能直接串并联化简。\n\n" +
                "【正确：外加电源法】\n" +
                "1. 独立源全部置零\n" +
                "2. 受控源保留\n" +
                "3. 端口外加测试电压Ut\n" +
                "4. 求端口电流It\n" +
                "5. Req = Ut / It\n\n" +
                "【口诀】独立源置零，受控源保留，外加电源求比值"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.BOUNDARY,
            chapter = "第4章-电路定理",
            tags = "节点法,网孔法,选择,对比",
            prompt = "什么时候不能用网孔法？",
            hint = "平面 vs 非平面",
            answer = "【限制】非平面电路不能用网孔法！\n\n" +
                "【选择标准：方程数少的优先】\n" +
                "- 节点法：n-1 个方程\n" +
                "- 网孔法：b-n+1 个方程\n\n" +
                "选节点法：电流源多、节点少、非平面\n" +
                "选网孔法：电压源多、网孔少、梯形电路"
        ))

        cardIds += cardRepository.createCard(Card(
            type = CardType.BOUNDARY,
            chapter = "第4章-电路定理",
            tags = "理想源,串联,并联,易错",
            prompt = "理想电压源和电流源的串并联有哪些禁忌？",
            hint = "矛盾组合 = 违反KVL/KCL",
            answer = "【禁忌1】不同电压源不能并联（违反KVL）\n\n" +
                "【禁忌2】不同电流源不能串联（违反KCL）\n\n" +
                "【合法】\n" +
                "- 相同电压源可并联\n" +
                "- 相同电流源可串联\n" +
                "- 不同电压源可串联：Us=Us1+Us2\n" +
                "- 不同电流源可并联：Is=Is1+Is2\n" +
                "- 电压源并联任何元件：端口电压=Us\n" +
                "- 电流源串联任何元件：支路电流=Is"
        ))

        // 建立连接关系
        graphRepository.addEdge(cardIds[1], cardIds[2], RelationType.EQUIVALENT, "戴维南和诺顿互为对偶")
        graphRepository.addEdge(cardIds[4], cardIds[1], RelationType.WORKFLOW, "源变换是求戴维南的中间步骤")
        graphRepository.addEdge(cardIds[1], cardIds[3], RelationType.REQUIRES, "最大功率需先求戴维南等效")
        graphRepository.addEdge(cardIds[5], cardIds[6], RelationType.CONTRADICTS, "同一题通常选其一")
        graphRepository.addEdge(cardIds[0], cardIds[10], RelationType.FAILS_WHEN, "求功率时不能直接叠加")
        graphRepository.addEdge(cardIds[1], cardIds[11], RelationType.EXTENDS, "含受控源时需用外加电源法")
        graphRepository.addEdge(cardIds[1], cardIds[7], RelationType.WORKFLOW, "概念理解后按步骤操作")
        graphRepository.addEdge(cardIds[9], cardIds[5], RelationType.WORKFLOW, "速查表指导选节点法")
        graphRepository.addEdge(cardIds[9], cardIds[6], RelationType.WORKFLOW, "速查表指导选网孔法")
        graphRepository.addEdge(cardIds[9], cardIds[1], RelationType.WORKFLOW, "速查表指导选戴维南")

        // 创建默认"电路"题库文件夹
        seedDefaultFolders(questionBankRepository)
    }

    suspend fun seedDefaultFolders(questionBankRepository: QuestionBankRepository) {
        if (questionBankRepository.getFolderCount() > 0) return
        questionBankRepository.createFolder("电路")
    }
}
