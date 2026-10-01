<template>
  <div class="page lf-page">
    <div class="page-head">
      <h2 class="page-title">
        失物保管与认领交接台
        <em>登记封存 · 同一条保管链 · 最小化审核 · 双人现场交还</em>
      </h2>
      <div class="page-tools">
        <el-radio-group v-model="role" size="small" @change="changeRole">
          <el-radio-button v-for="r in options.roles || []" :key="r.code" :value="r.code">
            {{ r.name }}
          </el-radio-button>
        </el-radio-group>
        <el-input v-model="operator" :placeholder="`当前${roleName}姓名`" style="width: 170px" />
      </div>
    </div>

    <div class="rule-tip">
      物品一经登记即生成封签并完成首次保管交接；查看、转移、开封、认领、异常、交还和处置都只向交接链追加事实。
      公开列表仅显示模糊类别、发现日期与馆内区域；完整照片、联系方式和认领证据仅对办理前台、值班经理在必要范围内展示。
      当前角色：<b>{{ roleName }}</b>。
    </div>

    <el-tabs v-model="activeTab" class="lf-tabs">
      <!-- 公开列表 -->
      <el-tab-pane label="公开失物" name="public">
        <div class="panel">
          <div class="panel-title">
            可公开浏览的保管中物品
            <small>不展示精确箭道、外观细节、照片或隐私特征，避免被直接猜中答案</small>
          </div>
          <el-table :data="publicItems" stripe>
            <el-table-column prop="itemNo" label="失物号" width="150" />
            <el-table-column prop="publicCategoryName" label="模糊类别" min-width="130" />
            <el-table-column label="发现日期" width="130">
              <template #default="{ row }">{{ shortDate(row.foundDate) }}</template>
            </el-table-column>
            <el-table-column prop="foundAreaName" label="馆内区域" min-width="120" />
            <el-table-column label="认领提示" min-width="260">
              <template #default>
                <span class="muted">请到前台描述遗失时间、可能区域及只有失主知晓的核验特征。</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 失物登记与保管链 -->
      <el-tab-pane label="失物登记 / 保管链" name="custody" :disabled="role === 'MEMBER'">
        <div v-if="role === 'FRONT'" class="panel">
          <div class="panel-title">
            发现登记与首次封存
            <small>提交后同事务生成失物封签、记录保管人与存放区域</small>
          </div>
          <el-form :model="registerForm" label-width="108px" class="lf-form">
            <div class="form-grid">
              <el-form-item label="物品类别" required>
                <el-select v-model="registerForm.category" style="width: 100%">
                  <el-option v-for="x in options.categories" :key="x.code" :label="x.name" :value="x.code" />
                </el-select>
              </el-form-item>
              <el-form-item label="发现区域" required>
                <el-select v-model="registerForm.foundAreaType" style="width: 100%">
                  <el-option v-for="x in options.areas" :key="x.code" :label="x.name" :value="x.code" />
                </el-select>
              </el-form-item>
              <el-form-item label="精确位置" required>
                <el-input v-model="registerForm.foundAreaDetail" placeholder="如 A03 箭道等候椅 / 3 号教室门口" />
              </el-form-item>
              <el-form-item label="发现时间" required>
                <el-date-picker
                  v-model="registerForm.foundAt"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="选择时间"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="发现人身份" required>
                <el-radio-group v-model="registerForm.finderType">
                  <el-radio v-for="x in options.finderTypes" :key="x.code" :value="x.code">{{ x.name }}</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="registerForm.finderType === 'MEMBER'" label="发现会员" required>
                <el-select filterable v-model="registerForm.finderMemberId" style="width: 100%">
                  <el-option v-for="m in members" :key="m.id" :label="`${m.cardNo} · ${m.name}`" :value="m.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="发现人姓名" required>
                <el-input v-model="registerForm.finderName" />
              </el-form-item>
              <el-form-item label="发现人联系">
                <el-input v-model="registerForm.finderContact" placeholder="工作人员可留工号或部门" />
              </el-form-item>
              <el-form-item label="外观特征" required>
                <el-input v-model="registerForm.appearance" type="textarea" :rows="2" placeholder="仅工作人员可见" />
              </el-form-item>
              <el-form-item label="现场照片" required>
                <el-select
                  v-model="registerForm.photoRefs"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  placeholder="输入照片编号或文件名后回车"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="当前保管人" required>
                <el-input v-model="registerForm.custodian" placeholder="首次交接接收人" />
              </el-form-item>
              <el-form-item label="存放区域" required>
                <el-input v-model="registerForm.storageArea" placeholder="如 前台失物柜 A 格" />
              </el-form-item>
              <el-form-item label="特殊标记">
                <el-checkbox v-model="registerForm.highValue">高价值物品</el-checkbox>
                <el-checkbox v-model="registerForm.identityItem">含身份信息</el-checkbox>
              </el-form-item>
            </div>
            <div class="form-actions">
              <el-button type="primary" :loading="saving" @click="submitRegister">登记、生成封签并完成首次交接</el-button>
            </div>
          </el-form>
        </div>

        <div v-if="role !== 'FRONT'" class="locked-tip">
          {{ role === 'MANAGER' ? '值班经理可查看完整保管链；登记、开封和转移由值班前台执行。' : '普通教练只能查看与交接相关的最小信息，不能浏览完整照片或联系方式。' }}
        </div>

        <div class="split-board">
          <div class="panel">
            <div class="panel-title">失物清单 <small>{{ items.length }} 件</small></div>
            <el-table :data="items" height="520" highlight-current-row @current-change="pickItem">
              <el-table-column prop="itemNo" label="编号" width="145" />
              <el-table-column label="类别" min-width="110">
                <template #default="{ row }">{{ role === 'COACH' ? row.publicCategoryName : row.categoryName }}</template>
              </el-table-column>
              <el-table-column label="区域" min-width="120">
                <template #default="{ row }">{{ row.foundAreaName }}</template>
              </el-table-column>
              <el-table-column label="封签" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.sealStatus === 'INTACT' ? 'success' : row.sealStatus === 'BROKEN' ? 'danger' : 'warning'">
                    {{ row.sealStatusName }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.overdue ? 'warning' : 'info'">{{ row.overdue && row.status === 'CUSTODY' ? '已逾期' : row.statusName }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="panel detail-panel" v-if="itemDetail">
            <div class="panel-title">
              {{ itemDetail.summary.itemNo }}
              <el-tag size="small">{{ itemDetail.summary.statusName }}</el-tag>
              <el-tag size="small" type="success">{{ itemDetail.summary.sealStatusName }}</el-tag>
            </div>
            <div v-if="role !== 'COACH'" class="info-grid">
              <div><span>精确位置</span><b>{{ itemDetail.summary.foundAreaDetail }}</b></div>
              <div><span>保管人</span><b>{{ itemDetail.summary.currentCustodian }}</b></div>
              <div><span>存放区</span><b>{{ itemDetail.summary.storageArea }}</b></div>
              <div><span>封签号</span><b class="mono">{{ itemDetail.summary.sealNo }}</b></div>
              <div><span>外观</span><b>{{ itemDetail.appearance }}</b></div>
              <div v-if="role === 'FRONT'"><span>照片</span><b>{{ itemDetail.photoRefs.join('、') || '-' }}</b></div>
              <div v-if="role === 'FRONT'"><span>发现人</span><b>{{ itemDetail.finderName }} {{ itemDetail.finderContact || '' }}</b></div>
              <div v-else><span>敏感资料</span><b>经理仅查看核验所需信息，完整照片/联系方式由前台保管</b></div>
              <div><span>保管期至</span><b>{{ itemDetail.summary.retentionUntil }}</b></div>
            </div>
            <div v-else class="locked-tip">普通教练仅可查看封签、状态和交接链，不展示照片、联系方式、外观细节。</div>

            <div class="action-row" v-if="canItem('TRANSFER')">
              <el-button size="small" @click="openTransfer">转移保管</el-button>
              <el-button size="small" @click="openInspect" v-if="canItem('INSPECT')">登记开封查看</el-button>
              <el-button size="small" @click="openReseal" v-if="canItem('RESEAL')">重新封存</el-button>
              <el-button size="small" type="primary" @click="goClaimForm" v-if="canItem('CREATE_CLAIM')">录入认领</el-button>
            </div>

            <el-divider content-position="left">封签记录</el-divider>
            <el-timeline>
              <el-timeline-item v-for="s in itemDetail.seals" :key="s.id" :timestamp="fmt(s.createdAt)" placement="top">
                <b>{{ s.actionName }}</b>
                <el-tag size="small">{{ s.beforeStatus || '—' }} → {{ s.afterStatus }}</el-tag>
                <p>{{ s.custodian }} / {{ s.storageArea }} · {{ s.reason }}</p>
              </el-timeline-item>
            </el-timeline>

            <el-divider content-position="left">不可变交接链</el-divider>
            <el-timeline>
              <el-timeline-item v-for="log in itemDetail.logs" :key="log.id" :timestamp="fmt(log.createdAt)" placement="top">
                <b>{{ log.actionName }}</b>
                <el-tag size="small" type="info">{{ log.roleName }} · {{ log.operator }}</el-tag>
                <p>{{ log.detail }}</p>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </el-tab-pane>

      <!-- 认领审核 -->
      <el-tab-pane label="认领审核" name="review" :disabled="role === 'MEMBER'">
        <div v-if="frontClaimForm.itemId" class="panel">
          <div class="panel-title">前台录入认领人信息 <small>会员或未建档访客均可</small></div>
          <el-form :model="frontClaimForm" label-width="110px">
            <div class="form-grid">
              <el-form-item label="认领人身份" required>
                <el-radio-group v-model="frontClaimForm.claimantType">
                  <el-radio value="MEMBER">现有会员</el-radio>
                  <el-radio value="GUEST">未建档访客</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="frontClaimForm.claimantType === 'MEMBER'" label="会员" required>
                <el-select filterable v-model="frontClaimForm.claimantMemberId" style="width: 100%">
                  <el-option v-for="m in members" :key="m.id" :label="`${m.cardNo} · ${m.name} · ${m.phone}`" :value="m.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="姓名" required><el-input v-model="frontClaimForm.claimantName" /></el-form-item>
              <el-form-item label="联系方式" required><el-input v-model="frontClaimForm.claimantContact" /></el-form-item>
              <el-form-item label="遗失时间范围" required>
                <el-date-picker v-model="claimRange" type="datetimerange" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
              </el-form-item>
              <el-form-item label="可能箭道/课程" required><el-input v-model="frontClaimForm.possibleAreas" /></el-form-item>
              <el-form-item label="认领描述" required><el-input v-model="frontClaimForm.description" type="textarea" :rows="2" /></el-form-item>
              <el-form-item label="隐私核验特征" required><el-input v-model="frontClaimForm.privacyFeature" placeholder="只填写完成核验所需最少信息" /></el-form-item>
              <el-form-item label="补充证明" required>
                <el-select v-model="frontClaimForm.evidenceRefs" multiple filterable allow-create default-first-option style="width:100%" placeholder="凭证文件名 / 照片编号" />
              </el-form-item>
            </div>
            <el-button type="primary" :loading="saving" @click="submitClaim">提交待经理审核</el-button>
            <el-button @click="frontClaimForm.itemId = null">取消</el-button>
          </el-form>
        </div>

        <div class="split-board">
          <div class="panel">
            <div class="panel-title">认领申请</div>
            <el-table :data="claims" height="560" highlight-current-row @current-change="pickClaim">
              <el-table-column prop="claimNo" label="申请号" width="145" />
              <el-table-column prop="itemNo" label="物品号" width="145" />
              <el-table-column prop="claimantName" label="认领人" min-width="100" />
              <el-table-column label="联系方式" min-width="130">
                <template #default="{ row }">{{ role === 'COACH' ? '***' : row.claimantContact }}</template>
              </el-table-column>
              <el-table-column label="轮次" width="60"><template #default="{ row }">{{ row.reviewRound }}</template></el-table-column>
              <el-table-column label="状态" width="95">
                <template #default="{ row }"><el-tag size="small">{{ row.statusName }}</el-tag></template>
              </el-table-column>
            </el-table>
          </div>

          <div class="panel detail-panel" v-if="claimDetail">
            <div class="panel-title">
              {{ claimDetail.summary.claimNo }}
              <el-tag>{{ claimDetail.summary.statusName }}</el-tag>
            </div>
            <div v-if="role !== 'COACH'" class="info-grid">
              <div><span>联系方式</span><b>{{ claimDetail.claimantContact }}</b></div>
              <div><span>时间范围</span><b>{{ fmt(claimDetail.lostTimeStart) }} 至 {{ fmt(claimDetail.lostTimeEnd) }}</b></div>
              <div><span>可能区域</span><b>{{ claimDetail.possibleAreas }}</b></div>
              <div><span>隐私特征</span><b>{{ claimDetail.privacyFeature }}</b></div>
              <div class="wide"><span>认领描述</span><b>{{ claimDetail.description }}</b></div>
              <div class="wide"><span>证明材料</span><b>{{ claimDetail.evidenceRefs.join('、') }}</b></div>
            </div>
            <div v-else class="locked-tip">普通教练不能查看完整照片、联系方式或认领证据；本页仅保留状态和流程进度。</div>

            <el-divider content-position="left">分项审核结论</el-divider>
            <el-table :data="claimDetail.reviews" size="small" border>
              <el-table-column prop="reviewRound" label="轮次" width="55" />
              <el-table-column prop="featureConclusionName" label="特征匹配" width="100" />
              <el-table-column prop="timePlaceConclusionName" label="时间地点" width="100" />
              <el-table-column prop="proofConclusionName" label="补充证明" width="100" />
              <el-table-column prop="resultName" label="结果" width="90" />
              <el-table-column prop="note" label="意见" min-width="160" />
            </el-table>

            <div v-if="canClaim('SUBMIT_SUPPLEMENT')" class="action-row">
              <el-button type="primary" size="small" @click="openSupplement">登记补证材料</el-button>
            </div>
            <div v-if="canClaim('REVIEW')" class="action-row">
              <el-button type="success" size="small" @click="openReview">值班经理审核</el-button>
            </div>
            <div v-if="canClaim('REPORT_EXCEPTION')" class="action-row">
              <el-button type="warning" size="small" @click="openException">现场发现异常，暂停交还</el-button>
            </div>

            <el-divider content-position="left">异常记录</el-divider>
            <el-timeline>
              <el-timeline-item v-for="e in claimDetail.exceptions" :key="e.id" :timestamp="fmt(e.createdAt)">
                <b>{{ e.reasonName }}</b>
                <el-tag size="small" :type="e.status === 'OPEN' ? 'danger' : 'info'">{{ e.statusName }}</el-tag>
                <p>{{ e.detail }}</p>
                <el-button v-if="e.actions.includes('RESOLVE_EXCEPTION')" size="small" type="warning" @click="openResolve(e)">
                  经理处理异常
                </el-button>
              </el-timeline-item>
            </el-timeline>

            <el-divider content-position="left">同链轨迹</el-divider>
            <el-timeline>
              <el-timeline-item v-for="log in claimDetail.logs" :key="log.id" :timestamp="fmt(log.createdAt)">
                <b>{{ log.actionName }}</b>
                <el-tag size="small" type="info">{{ log.roleName }} · {{ log.operator }}</el-tag>
                <p>{{ log.detail }}</p>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </el-tab-pane>

      <!-- 现场交还 -->
      <el-tab-pane label="现场交还" name="return" :disabled="role === 'MEMBER'">
        <div class="panel">
          <div class="panel-title">待交还申请 <small>必须当前保管人 + 另一名工作人员共同完成</small></div>
          <el-table :data="returnClaims" stripe>
            <el-table-column prop="claimNo" label="申请号" width="150" />
            <el-table-column prop="itemNo" label="物品号" width="150" />
            <el-table-column prop="claimantName" label="登记认领人" min-width="120" />
            <el-table-column prop="statusName" label="状态" width="110" />
            <el-table-column prop="submittedBy" label="受理前台" min-width="110" />
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="pickClaim(row)">查看</el-button>
                <el-button v-if="row.actions.includes('RETURN')" size="small" type="success" @click="openReturn(row)">
                  双人交还
                </el-button>
                <el-button v-if="row.actions.includes('REPORT_EXCEPTION')" size="small" type="warning" @click="pickClaim(row); activeTab='review'">
                  异常
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 逾期处置 -->
      <el-tab-pane label="逾期处置" name="disposal" :disabled="role !== 'MANAGER'">
        <div class="panel">
          <div class="panel-title">
            待处置清单
            <small>高价值、带身份信息物品必须走对应公安移交路径并登记凭证</small>
          </div>
          <el-table :data="overdueItems" stripe>
            <el-table-column prop="itemNo" label="物品号" width="150" />
            <el-table-column prop="categoryName" label="类别" min-width="110" />
            <el-table-column prop="foundAreaName" label="发现区域" min-width="100" />
            <el-table-column prop="retentionUntil" label="保管期至" width="120" />
            <el-table-column label="特殊路径" min-width="180">
              <template #default="{ row }">
                <el-tag v-if="row.highValue && row.identityItem" type="danger">高价值+身份信息</el-tag>
                <el-tag v-else-if="row.highValue" type="warning">高价值</el-tag>
                <el-tag v-else-if="row.identityItem" type="warning">含身份信息</el-tag>
                <span v-else class="muted">普通路径</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }"><el-button size="small" type="primary" @click="openDispose(row)">登记处置</el-button></template>
            </el-table-column>
          </el-table>
        </div>
        <div class="panel" style="margin-top:14px">
          <div class="panel-title">处置台账</div>
          <el-table :data="disposals" stripe>
            <el-table-column prop="disposalNo" label="处置号" width="150" />
            <el-table-column prop="disposalPathName" label="处置路径" min-width="210" />
            <el-table-column prop="receiverName" label="接收方" min-width="130" />
            <el-table-column prop="credentialNo" label="凭证号" min-width="130" />
            <el-table-column prop="disposedBy" label="经理" min-width="100" />
            <el-table-column label="时间" min-width="160"><template #default="{ row }">{{ fmt(row.disposedAt) }}</template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 通用动作弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="560px">
      <el-form :model="dialog.form" label-width="110px">
        <template v-for="f in dialog.fields" :key="f.key">
          <el-form-item :label="f.label">
            <el-select v-if="f.type === 'select'" v-model="dialog.form[f.key]" style="width:100%">
              <el-option v-for="o in f.options" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
            <el-select v-else-if="f.type === 'tags'" v-model="dialog.form[f.key]" multiple filterable allow-create default-first-option style="width:100%" />
            <el-input v-else-if="f.type === 'textarea'" v-model="dialog.form[f.key]" type="textarea" :rows="3" />
            <el-input v-else v-model="dialog.form[f.key]" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmDialog">确认并追加交接记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { lostFoundApi, memberApi, shortDate } from '../api'

const role = ref('FRONT')
const operator = ref('前台-马倩')
const activeTab = ref('public')
const saving = ref(false)
const options = ref({ roles: [] })
const members = ref([])
const publicItems = ref([])
const items = ref([])
const claims = ref([])
const disposals = ref([])
const itemDetail = ref(null)
const claimDetail = ref(null)
const claimRange = ref([])

const emptyRegister = () => ({
  category: 'ELECTRONICS', foundAreaType: 'LANE', foundAreaDetail: '', foundAt: '',
  appearance: '', photoRefs: [], finderType: 'STAFF', finderMemberId: null,
  finderName: '', finderContact: '', custodian: operator.value, storageArea: '',
  highValue: false, identityItem: false
})
const registerForm = reactive(emptyRegister())
const frontClaimForm = reactive({
  itemId: null, claimantType: 'MEMBER', claimantMemberId: null, claimantName: '', claimantContact: '',
  description: '', possibleAreas: '', privacyFeature: '', evidenceRefs: []
})
const dialog = reactive({ visible: false, title: '', action: '', id: null, form: {}, fields: [] })

const roleName = computed(() => (options.value.roles || []).find((x) => x.code === role.value)?.name || role.value)
const overdueItems = computed(() => items.value.filter((x) => x.overdue && x.status === 'CUSTODY'))
const returnClaims = computed(() => claims.value.filter((x) => ['APPROVED', 'EXCEPTION'].includes(x.status)))

function fmt(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : ''
}
function requestKey(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
}
function canItem(action) {
  return itemDetail.value?.actions.includes(action)
}
function canClaim(action) {
  return claimDetail.value?.summary.actions.includes(action)
}
function baseActor() {
  return { role: role.value, operator: operator.value, requestKey: requestKey('op') }
}

async function loadPublic() {
  publicItems.value = await lostFoundApi.publicItems()
}
async function loadInternal() {
  if (role.value === 'MEMBER') {
    items.value = []
    claims.value = []
    disposals.value = []
    return
  }
  const [itemList, claimList] = await Promise.all([
    lostFoundApi.items(role.value),
    lostFoundApi.claims(role.value)
  ])
  items.value = itemList
  claims.value = claimList
  if (role.value === 'MANAGER') disposals.value = await lostFoundApi.disposals(role.value)
  else disposals.value = []
}
async function refreshAll() {
  await Promise.all([loadPublic(), loadInternal()])
  if (itemDetail.value) await pickItem(items.value.find((x) => x.id === itemDetail.value.summary.id))
  if (claimDetail.value) await pickClaim(claims.value.find((x) => x.id === claimDetail.value.summary.id))
}
async function changeRole() {
  itemDetail.value = null
  claimDetail.value = null
  if (role.value === 'MEMBER') activeTab.value = 'public'
  await refreshAll()
}
async function pickItem(row) {
  if (!row || !row.id) return
  itemDetail.value = await lostFoundApi.item(row.id, role.value)
}
async function pickClaim(row) {
  if (!row || !row.id) return
  claimDetail.value = await lostFoundApi.claim(row.id, role.value)
}
async function loadOptions() {
  options.value = await lostFoundApi.options()
  members.value = await memberApi.options()
}

async function submitRegister() {
  saving.value = true
  try {
    const data = { ...registerForm, role: role.value, operator: operator.value, requestKey: requestKey('register') }
    const result = await lostFoundApi.register(data)
    ElMessage.success('登记与首次封存完成')
    Object.assign(registerForm, emptyRegister())
    await refreshAll()
    itemDetail.value = result
    activeTab.value = 'custody'
  } finally {
    saving.value = false
  }
}

function goClaimForm() {
  frontClaimForm.itemId = itemDetail.value.summary.id
  activeTab.value = 'review'
}
async function submitClaim() {
  if (!claimRange.value?.length) return ElMessage.warning('请选择遗失时间范围')
  saving.value = true
  try {
    await lostFoundApi.createClaim(frontClaimForm.itemId, {
      ...frontClaimForm,
      lostTimeStart: claimRange.value[0],
      lostTimeEnd: claimRange.value[1],
      role: 'FRONT',
      operator: operator.value,
      requestKey: requestKey('claim')
    })
    ElMessage.success('认领申请已提交')
    frontClaimForm.itemId = null
    claimRange.value = []
    await refreshAll()
  } finally {
    saving.value = false
  }
}

function openTransfer() {
  showDialog('转移保管', 'transfer', itemDetail.value.summary.id, [
    { key: 'newCustodian', label: '新保管人' },
    { key: 'newStorageArea', label: '新存放区域' },
    { key: 'reason', label: '转移原因', type: 'textarea' }
  ], { newCustodian: '', newStorageArea: '', reason: '' })
}
function openInspect() {
  showDialog('开封查看', 'inspect', itemDetail.value.summary.id, [
    { key: 'reason', label: '开封原因', type: 'textarea' }
  ], { reason: '' })
}
function openReseal() {
  showDialog('重新封存', 'reseal', itemDetail.value.summary.id, [
    { key: 'newSealNo', label: '新封签号' },
    { key: 'storageArea', label: '存放区域' },
    { key: 'reason', label: '封存原因', type: 'textarea' }
  ], { newSealNo: '', storageArea: itemDetail.value.summary.storageArea, reason: '' })
}
function openSupplement() {
  showDialog('登记补证', 'supplement', claimDetail.value.summary.id, [
    { key: 'evidenceRefs', label: '补证材料', type: 'tags' },
    { key: 'note', label: '补证说明', type: 'textarea' }
  ], { evidenceRefs: [], note: '' })
}
function openReview() {
  showDialog('三项分别审核', 'review', claimDetail.value.summary.id, [
    { key: 'featureConclusion', label: '特征匹配', type: 'select', options: options.value.conclusions },
    { key: 'timePlaceConclusion', label: '时间地点', type: 'select', options: options.value.conclusions },
    { key: 'proofConclusion', label: '补充证明', type: 'select', options: options.value.conclusions },
    { key: 'result', label: '审核结果', type: 'select', options: [
      { code: 'APPROVED', name: '通过，进入待交还' },
      { code: 'SUPPLEMENT', name: '要求补证' },
      { code: 'REJECTED', name: '驳回' }
    ]},
    { key: 'note', label: '审核意见', type: 'textarea' }
  ], { featureConclusion: 'PASS', timePlaceConclusion: 'PASS', proofConclusion: 'PASS', result: 'APPROVED', note: '' })
}
function openException() {
  showDialog('异常交接', 'exception', claimDetail.value.summary.id, [
    { key: 'reason', label: '异常原因', type: 'select', options: options.value.exceptionReasons },
    { key: 'detail', label: '异常情况', type: 'textarea' }
  ], { reason: 'BROKEN_SEAL', detail: '' })
}
function openResolve(exception) {
  showDialog('经理处理异常', 'resolveException', exception.id, [
    { key: 'resolution', label: '处理方式', type: 'select', options: [
      { code: 'RESEAL', name: '重新封存后回到待交还' },
      { code: 'REVERIFY_PASS', name: '补充核验通过' },
      { code: 'TERMINATE', name: '终止本次认领' }
    ]},
    { key: 'newSealNo', label: '新封签号' },
    { key: 'storageArea', label: '存放区域' },
    { key: 'newCustodian', label: '必要时更换保管人' },
    { key: 'note', label: '处理说明', type: 'textarea' }
  ], { resolution: 'RESEAL', newSealNo: '', storageArea: '', newCustodian: '', note: '' })
}
function openReturn(row) {
  showDialog('双人现场交还', 'return', row.id, [
    { key: 'witness', label: '另一名工作人员' },
    { key: 'actualReceiver', label: '实际领取人' },
    { key: 'receiverRelation', label: '与认领人关系' },
    { key: 'idVerifyMethod', label: '证件核验方式' },
    { key: 'note', label: '交接备注', type: 'textarea' }
  ], { witness: '', actualReceiver: row.claimantName, receiverRelation: '本人', idVerifyMethod: '身份证原件+手机号', note: '' })
}
function openDispose(row) {
  showDialog('逾期处置登记', 'dispose', row.id, [
    { key: 'method', label: '处置结果', type: 'select', options: options.value.disposalMethods },
    { key: 'receiverName', label: '接收方' },
    { key: 'credentialNo', label: '凭证号' },
    { key: 'credentialRefs', label: '凭证附件', type: 'tags' },
    { key: 'note', label: '说明', type: 'textarea' }
  ], { method: row.highValue || row.identityItem ? 'POLICE' : 'DONATE', receiverName: '', credentialNo: '', credentialRefs: [], note: '' })
}
function showDialog(title, action, id, fields, form) {
  dialog.title = title
  dialog.action = action
  dialog.id = id
  dialog.fields = fields
  dialog.form = { ...form, ...baseActor() }
  dialog.visible = true
}
async function confirmDialog() {
  saving.value = true
  try {
    const api = lostFoundApi
    const action = dialog.action
    const id = dialog.id
    const data = { ...dialog.form }
    if (action === 'transfer') await api.transfer(itemDetail.value.summary.id, data)
    if (action === 'inspect') await api.inspect(itemDetail.value.summary.id, data)
    if (action === 'reseal') await api.reseal(itemDetail.value.summary.id, data)
    if (action === 'supplement') await api.supplement(id, data)
    if (action === 'review') await api.review(id, data)
    if (action === 'exception') await api.exception(id, data)
    if (action === 'resolveException') await api.resolveException(id, data)
    if (action === 'return') await api.returnItem(id, data)
    if (action === 'dispose') await api.dispose(id, data)
    ElMessage.success('操作完成，交接链已追加')
    dialog.visible = false
    await refreshAll()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadOptions()
  await refreshAll()
})
</script>

<style scoped>
.lf-page .page-tools {
  display: flex;
  gap: 10px;
}
.lf-tabs {
  margin-top: 10px;
}
.panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 8px 24px rgba(25, 45, 42, 0.05);
}
.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  margin-bottom: 12px;
}
.panel-title small {
  color: var(--ink-mute);
  font-weight: 400;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}
.form-actions,
.action-row {
  display: flex;
  gap: 8px;
  margin: 10px 0;
}
.split-board {
  display: grid;
  grid-template-columns: minmax(420px, 0.9fr) minmax(520px, 1.1fr);
  gap: 14px;
  margin-top: 14px;
}
.detail-panel {
  max-height: calc(100vh - 210px);
  overflow: auto;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.info-grid div {
  background: #f7faf9;
  border-radius: 10px;
  padding: 8px 10px;
}
.info-grid .wide {
  grid-column: 1 / -1;
}
.info-grid span {
  display: block;
  color: var(--ink-mute);
  font-size: 12px;
  margin-bottom: 4px;
}
.info-grid b {
  font-weight: 600;
  word-break: break-all;
}
.locked-tip {
  background: #fff8e8;
  border: 1px solid #f2d48a;
  color: #7a5b15;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
.muted {
  color: var(--ink-mute);
}
:deep(.el-timeline-item p) {
  margin: 4px 0 0;
  color: var(--ink-soft);
}
@media (max-width: 1180px) {
  .form-grid,
  .split-board,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
