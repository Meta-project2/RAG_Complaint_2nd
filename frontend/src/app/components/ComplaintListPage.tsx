import { useState, useEffect } from 'react';
import { Search, Filter, X, Eye, Loader2, Globe, User, UserCheck, FileText, Sparkles } from 'lucide-react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Badge } from './ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from './ui/table';
import { Checkbox } from './ui/checkbox';
import { AgentComplaintApi, ComplaintDto } from '../../api/AgentComplaintApi';
import { toast } from 'sonner';
import { Card } from './ui/card';

interface ComplaintListPageProps {
  onViewDetail: (id: string) => void;
}

const statusMap: Record<string, { label: string; color: string }> = {
  RECEIVED: { label: '접수', color: 'bg-blue-100 text-blue-800' },
  NORMALIZED: { label: '정규화', color: 'bg-purple-100 text-purple-800' },
  RECOMMENDED: { label: '추천완료', color: 'bg-cyan-100 text-cyan-800' },
  IN_PROGRESS: { label: '처리중', color: 'bg-yellow-100 text-yellow-800' },
  RESOLVED: { label: '처리완료', color: 'bg-green-100 text-green-800' },
  CLOSED: { label: '종결', color: 'bg-green-100 text-green-800' },
};

const urgencyMap: Record<string, { label: string; color: string }> = {
  HIGH: { label: '높음', color: 'bg-red-100 text-red-700' },
  MEDIUM: { label: '보통', color: 'bg-orange-100 text-orange-700' },
  LOW: { label: '낮음', color: 'bg-slate-100 text-slate-700' },
};

const tagMap: Record<string, string> = {
  악성: 'bg-red-100 text-red-700 border-red-300',
  반복: 'bg-orange-100 text-orange-700 border-orange-300',
  지역급증: 'bg-purple-100 text-purple-700 border-purple-300',
  키워드급증: 'bg-blue-100 text-blue-700 border-blue-300',
};

export function ComplaintListPage({ onViewDetail }: ComplaintListPageProps) {
  const [complaints, setComplaints] = useState<ComplaintDto[]>([]);
  const [loading, setLoading] = useState(true);

  // 필터 상태
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<string>('all');
  const [selectedUrgency, setSelectedUrgency] = useState<string>('all');
  const [includeIncidents, setIncludeIncidents] = useState(false);
  const [tagsOnly, setTagsOnly] = useState(false);

  const [selectedComplaintId, setSelectedComplaintId] = useState<number | null>(null);

  // 필터 변경 시 자동 검색
  useEffect(() => {
    fetchData();
  }, [selectedStatus, selectedUrgency, includeIncidents, tagsOnly]);

  const fetchData = async () => {
    try {
      setLoading(true);

      const params: any = {};
      if (searchQuery) params.keyword = searchQuery;
      if (selectedStatus !== 'all') params.status = selectedStatus;
      if (selectedUrgency !== 'all') params.urgency = selectedUrgency;
      if (includeIncidents) params.hasIncident = true;
      if (tagsOnly) params.hasTags = true;

      const data = await AgentComplaintApi.getAll(params);
      setComplaints(data);
    } catch (error) {
      console.error("민원 목록 로딩 실패:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      fetchData();
    }
  };


  // 필터 리셋
  const resetFilters = () => {
    setSelectedStatus('all');
    setSelectedUrgency('all');
    setIncludeIncidents(false);
    setTagsOnly(false);
  };

  // 담당자 배정 핸들러
  const handleAssign = async (e: React.MouseEvent, id: number) => {
    e.stopPropagation();
    try {
      await AgentComplaintApi.assign(id);
      toast.success("담당자로 배정되었습니다.");
      fetchData(); // 상태 갱신을 위해 목록 다시 불러오기
    } catch (error) {
      toast.error("배정 실패: 이미 처리 중이거나 오류가 발생했습니다.");
    }
  };

  const selectedComplaintData = complaints.find((c) => c.id === selectedComplaintId);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="animate-spin h-8 w-8 text-primary" />
      </div>
    );
  }

  return (
    <div className="flex h-full">
      {/* Main Content */}
      <div className={`flex-1 flex flex-col ${selectedComplaintId ? '' : ''}`}>
        {/* mr-80 - 미리보기 메인페이지 너비 조정시 사용 */}

        {/* 상단 헤더 */}
        {/* <div className="h-16 border-b border-border bg-card px-6 py-4">
          <div className="mb-1"><h1>민원함</h1> <p className="text-sm text-muted-foreground">내 부서 배정 민원</p></div>
          
        </div> */}
        <div className="h-16 border-b border-border bg-card px-6 shadow-sm flex items-center gap-3">
          <h1 className="text-2.5xl font-bold text-slate-900">민원함</h1>
          <p className="text-sm text-slate-400 font-medium pt-1">내 부서 배정 민원</p>
        </div>

        {/* 필터 영역 */}

        {/* 테이블 영역 */}
        <div className="flex-1 overflow-auto p-6 bg-slate-100/50">
          <div className=" py-3 flex items-center gap-4 justify-left">
            <div className="flex gap-2">
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="제목, 내용 검색"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyDown={handleKeyDown}
                  className="pl-9 bg-input-background pr-8"
                />
                {searchQuery && (
                  <button
                    onClick={() => { setSearchQuery(''); fetchData(); }}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
              
            </div>
            <div>
              <Button className='border-2' variant="outline" onClick={fetchData}>검색</Button>
            </div>
            <div className="flex flex-wrap gap-2 items-center">
              <Select value={selectedStatus} onValueChange={setSelectedStatus}>
                <SelectTrigger className="w-32 bg-input-background">
                  <SelectValue placeholder="상태" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 상태</SelectItem>
                  <SelectItem value="RECEIVED">접수</SelectItem>
                  <SelectItem value="NORMALIZED">정규화</SelectItem>
                  <SelectItem value="IN_PROGRESS">처리중</SelectItem>
                  <SelectItem value="CLOSED">종결</SelectItem>
                </SelectContent>
              </Select>

              {/* <Select value={selectedUrgency} onValueChange={setSelectedUrgency}>
              <SelectTrigger className="w-32 bg-input-background">
                <SelectValue placeholder="긴급도" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">전체 긴급도</SelectItem>
                <SelectItem value="HIGH">높음</SelectItem>
                <SelectItem value="MEDIUM">보통</SelectItem>
                <SelectItem value="LOW">낮음</SelectItem>
              </SelectContent>
            </Select> */}

              <div className="flex items-center space-x-2 px-3 py-2 border rounded bg-input-background">
                <Checkbox
                  id="incidents"
                  checked={includeIncidents}
                  onCheckedChange={(checked) => setIncludeIncidents(checked as boolean)}
                />
                <label htmlFor="incidents" className="text-sm cursor-pointer select-none">
                  사건 포함
                </label>
              </div>

              <div className="flex items-center space-x-2 px-3 py-2 border rounded bg-input-background">
                <Checkbox
                  id="tags"
                  checked={tagsOnly}
                  onCheckedChange={(checked) => setTagsOnly(checked as boolean)}
                />
                <label htmlFor="tags" className="text-sm cursor-pointer select-none">
                  특이 태그만
                </label>
              </div>

              <Button variant="ghost" size="sm" className="ml-auto" onClick={resetFilters}>
                <X className="h-4 w-4 mr-1" />
                필터 초기화
              </Button>
            </div>
          </div>

          <Card className="flex-1 flex flex-col overflow-hidden border-none shadow-md bg-white rounded-md">
            <div className="flex-1 overflow-auto">
              <Table>
                <TableHeader className="sticky top-0 bg-slate-300 border-b-2 z-10">
                  <TableRow>
                    <TableHead className="w-[160px] text-center font-bold text-slate-900 border-r border-slate-400">민원번호</TableHead>
                    <TableHead className="text-center font-bold text-slate-900 border-r border-slate-400">제목</TableHead>
                    <TableHead className="w-[160px] text-center font-bold text-slate-900 border-r border-slate-400">접수일시</TableHead>
                    {/* <TableHead className="w-[100px] text-center">긴급도</TableHead> */}
                    <TableHead className="w-[100px] text-center font-bold text-slate-900 border-r border-slate-400">상태</TableHead>
                    <TableHead className="w-[120px] text-center font-bold text-slate-900 border-r border-slate-400">사건</TableHead>
                    <TableHead className="text-center font-bold text-slate-900 border-r border-slate-400">특이태그</TableHead>
                    <TableHead className="text-center font-bold text-slate-900 border-r border-slate-400">담당자</TableHead>
                    <TableHead className="text-center font-bold text-slate-900">관리</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {complaints.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center py-10 text-muted-foreground">
                        접수된 민원이 없습니다.
                      </TableCell>
                    </TableRow>
                  ) : (
                    complaints.map((c) => (
                      <TableRow
                        key={c.id}
                        className={`cursor-pointer hover:bg-slate-100 ${selectedComplaintId === c.id ? 'bg-blue-50/80' : 'border-b border-slate-200'}`} // 선택 시 색상도 살짝 조정
                        onClick={() => setSelectedComplaintId(c.id)}
                      >
                        <TableCell className="text-sm text-muted-foreground text-center font-mono">
                          {c.id}
                        </TableCell>

                        <TableCell className="">
                          <div className="flex flex-col">
                            <span className="font-medium">{c.title}</span>
                          </div>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground text-center">
                          {c.receivedAt || '-'}
                        </TableCell>
                        {/* <TableCell>
                        <Badge className={urgencyMap[c.urgency]?.color || 'bg-gray-100'}>
                          {urgencyMap[c.urgency]?.label || c.urgency}
                        </Badge>
                      </TableCell> */}
                        <TableCell className="text-center">
                          <div className="flex justify-center">
                            <Badge className={statusMap[c.status]?.color || 'bg-gray-100'}>
                              {statusMap[c.status]?.label || c.status}
                            </Badge>
                          </div>
                        </TableCell>
                        <TableCell className="text-center">
                          <div className="flex justify-center">
                            {c.incidentId ? (
                              <Badge variant="secondary" className="font-mono text-xs">
                                {c.incidentId}
                              </Badge>
                            ) : (
                              <span className="text-muted-foreground text-xs">-</span>
                            )}
                          </div>
                        </TableCell>

                        <TableCell className="text-center">
                          <div className="flex justify-center gap-1 flex-wrap">
                            {c.tags && c.tags.length > 0 ? (
                              c.tags.map((tag) => (
                                <Badge key={tag} variant="outline" className={tagMap[tag] || 'bg-gray-100'}>
                                  {tag}
                                </Badge>
                              ))
                            ) : (
                              <span className="text-xs text-muted-foreground">-</span>
                            )}
                          </div>
                        </TableCell>

                        <TableCell className="text-center">
                          {c.managerName ? (
                            <div className="flex items-center justify-center gap-1.5">
                              <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center border">
                                <User className="h-3 w-3 text-slate-500" />
                              </div>
                              <span className="text-sm font-medium text-slate-700">{c.managerName}</span>
                            </div>
                          ) : (
                            <span className="text-xs text-slate-400">미지정</span>
                          )}
                        </TableCell>


                        <TableCell className="text-center">
                          <div className="flex items-center justify-center gap-2">
                            {/* 담당자가 없을 때만 '담당하기' 버튼 표시 */}
                            {!c.managerName && (
                              // <Button
                              //   size="sm"
                              //   className="h-8 bg-gray-400 hover:bg-gray-500 shadow-sm text-xs px-3"
                              //   onClick={(e) => handleAssign(e, c.id)}
                              // >
                              //   <UserCheck className="h-3 w-3 mr-1.5" /> 담당하기
                              // </Button>
                              <Button
                                size="sm"
                                className="bg-blue-600 hover:bg-blue-700"
                                onClick={(e) => handleAssign(e, c.id)}
                              >
                                <UserCheck className="h-3 w-3 mr-1.5" /> 담당하기
                              </Button>

                            )}

                            {/* '열기' 버튼은 항상 표시 */}
                            <Button
                              size="sm"
                              variant="ghost"
                              className="h-8 text-xs px-3 border border-slate-200"
                              onClick={(e) => { e.stopPropagation(); onViewDetail(String(c.id)); }}
                            >
                              <Eye className="h-3 w-3 mr-1.5" /> 열기
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </Card>
        </div>
      </div>

      {/* 우측 미리보기 패널 */}
      {selectedComplaintId && selectedComplaintData && (
        <div className="fixed right-0 top-0 h-screen w-80 bg-white border-l border-slate-200 shadow-2xl z-50 overflow-y-auto">
          <div className="px-4 h-16 border-b border-border flex items-center justify-between">
            <h3 className="text-sm">미리보기</h3>
            <Button
              variant="ghost"
              size="icon"
              className="h-6 w-6"
              onClick={() => setSelectedComplaintId(null)}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          <div className="p-4 space-y-4 gird">
            <div>
              <div className="text-xs text-muted-foreground mb-1">민원 ID</div>
              <div className="text-sm">{selectedComplaintData.id}</div>
            </div>

            <div>
              <div className="text-xs text-muted-foreground mb-1">사건 연결</div>
              {selectedComplaintData.incidentId ? (
                <Badge variant="secondary">{selectedComplaintData.incidentId}</Badge>
              ) : (
                <span className="text-sm text-muted-foreground">미연결</span>
              )}
            </div>

            <div>
              <div className="text-xs text-muted-foreground mb-1">제목</div>
              <div className="text-sm">{selectedComplaintData.title}</div>
            </div>

            <div>
              <div className="text-xs text-muted-foreground mb-2"><Sparkles className="w-3 h-3 mr-1"/>AI 요약</div>
              <div className="text-sm p-3 bg-muted rounded border min-h-[80px]">
                {selectedComplaintData.neutralSummary ? (
                  <p className="leading-relaxed">{selectedComplaintData.neutralSummary}</p>
                ) : (
                  <div className="flex h-full items-center justify-center text-muted-foreground text-xs">
                    요약 내용 없음
                  </div>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <div className="text-xs text-muted-foreground mb-1">위치</div>
                <div className="text-sm">{selectedComplaintData.addressText || '-'}</div>
              </div>
              {/* <div>
                <div className="text-xs text-muted-foreground mb-1">긴급도</div>
                <Badge className={urgencyMap[selectedComplaintData.urgency]?.color}>
                  {urgencyMap[selectedComplaintData.urgency]?.label}
                </Badge>
              </div> */}
            </div>


            {/* 
            <div className="pt-4 space-y-2">
              <Button className="w-full" onClick={() => onViewDetail(String(selectedComplaintData.id))}>
                상세 열기
              </Button>
              <Button variant="outline" className="w-full">
                재이관 요청
              </Button>
            </div> */}
          </div>
        </div>
      )}
    </div>
  );
}