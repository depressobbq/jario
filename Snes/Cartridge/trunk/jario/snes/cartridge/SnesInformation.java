package jario.snes.cartridge;

public class SnesInformation
{
	public String xml_memory_map;

    public SnesInformation(byte[] data, int size)
    {
        read_header(data, size);

        String xml = "<?xml version='1.0' encoding='UTF-8'?>\n";
        
//        if (type == Type.Bsx)
//        {
//            xml += "<cartridge/>";
//            xml_memory_map = xml;
//            return;
//        }
//
//        if (type == Type.SufamiTurbo)
//        {
//            xml += "<cartridge/>";
//            xml_memory_map = xml;
//            return;
//        }
//
//        if (type == Type.GameBoy)
//        {
//            xml += "<cartridge rtc='" + gameboy_has_rtc(data, size) + "'>\n";
//            if (gameboy_ram_size(data, size) > 0)
//            {
//                xml += "  <ram size='" + gameboy_ram_size(data, size).ToString("X") + "'/>\n";
//            }
//            xml += "</cartridge>\n";
//            xml_memory_map = xml;
//            return;
//        }

        xml += "<cartridge";
        if (region == Region.NTSC)
        {
            xml += " region='NTSC'";
        }
        else
        {
            xml += " region='PAL'";
        }
        xml += ">\n";

        if (mapper == MemoryMapper.LoROM)
        {
            xml += "  <rom>\n";
            xml += "    <map mode='linear' address='00-7f:8000-ffff'/>\n";
            xml += "    <map mode='linear' address='80-ff:8000-ffff'/>\n";
            xml += "  </rom>\n";

            if (ram_size > 0)
            {
                xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
                xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
                xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
                if ((rom_size > 0x200000) || (ram_size > 32 * 1024))
                {
                    xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
                    xml += "    <map mode='linear' address='f0-ff:0000-7fff'/>\n";
                }
                else
                {
                    xml += "    <map mode='linear' address='70-7f:0000-ffff'/>\n";
                    xml += "    <map mode='linear' address='f0-ff:0000-ffff'/>\n";
                }
                xml += "  </ram>\n";
            }
        }
        else if (mapper == MemoryMapper.HiROM)
        {
            xml += "  <rom>\n";
            xml += "    <map mode='shadow' address='00-3f:8000-ffff'/>\n";
            xml += "    <map mode='linear' address='40-7f:0000-ffff'/>\n";
            xml += "    <map mode='shadow' address='80-bf:8000-ffff'/>\n";
            xml += "    <map mode='linear' address='c0-ff:0000-ffff'/>\n";
            xml += "  </rom>\n";

            if (ram_size > 0)
            {
                xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
                xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
                xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
                if ((rom_size > 0x200000) || (ram_size > 32 * 1024))
                {
                    xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
                }
                else
                {
                    xml += "    <map mode='linear' address='70-7f:0000-ffff'/>\n";
                }
                xml += "  </ram>\n";
            }
        }
        else if (mapper == MemoryMapper.ExLoROM)
        {
            xml += "  <rom>\n";
            xml += "    <map mode='linear' address='00-3f:8000-ffff'/>\n";
            xml += "    <map mode='linear' address='40-7f:0000-ffff'/>\n";
            xml += "    <map mode='linear' address='80-bf:8000-ffff'/>\n";
            xml += "  </rom>\n";

            if (ram_size > 0)
            {
                xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
                xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
                xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
                xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
                xml += "  </ram>\n";
            }
        }
        else if (mapper == MemoryMapper.ExHiROM)
        {
            xml += "  <rom>\n";
            xml += "    <map mode='shadow' address='00-3f:8000-ffff' offset='400000'/>\n";
            xml += "    <map mode='linear' address='40-7f:0000-ffff' offset='400000'/>\n";
            xml += "    <map mode='shadow' address='80-bf:8000-ffff' offset='000000'/>\n";
            xml += "    <map mode='linear' address='c0-ff:0000-ffff' offset='000000'/>\n";
            xml += "  </rom>\n";

            if (ram_size > 0)
            {
                xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
                xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
                xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
                if ((rom_size > 0x200000) || (ram_size > 32 * 1024))
                {
                    xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
                }
                else
                {
                    xml += "    <map mode='linear' address='70-7f:0000-ffff'/>\n";
                }
                xml += "  </ram>\n";
            }
        }
        else if (mapper == MemoryMapper.BSCLoROM)
        {
            xml += "  <rom>\n";
            xml += "    <map mode='linear' address='00-1f:8000-ffff' offset='000000'/>\n";
            xml += "    <map mode='linear' address='20-3f:8000-ffff' offset='100000'/>\n";
            xml += "    <map mode='linear' address='80-9f:8000-ffff' offset='200000'/>\n";
            xml += "    <map mode='linear' address='a0-bf:8000-ffff' offset='100000'/>\n";
            xml += "  </rom>\n";
            xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
            xml += "    <map mode='linear' address='70-7f:0000-7fff'/>\n";
            xml += "    <map mode='linear' address='f0-ff:0000-7fff'/>\n";
            xml += "  </ram>\n";
            xml += "  <bsx>\n";
            xml += "    <slot>\n";
            xml += "      <map mode='linear' address='c0-ef:0000-ffff'/>\n";
            xml += "    </slot>\n";
            xml += "  </bsx>\n";
        }
        else if (mapper == MemoryMapper.BSCHiROM)
        {
            xml += "  <rom>\n";
            xml += "    <map mode='shadow' address='00-1f:8000-ffff'/>\n";
            xml += "    <map mode='linear' address='40-5f:0000-ffff'/>\n";
            xml += "    <map mode='shadow' address='80-9f:8000-ffff'/>\n";
            xml += "    <map mode='linear' address='c0-df:0000-ffff'/>\n";
            xml += "  </rom>\n";
            xml += "  <ram size='" + Integer.toHexString(ram_size) + "'>\n";
            xml += "    <map mode='linear' address='20-3f:6000-7fff'/>\n";
            xml += "    <map mode='linear' address='a0-bf:6000-7fff'/>\n";
            xml += "  </ram>\n";
            xml += "  <bsx>\n";
            xml += "    <slot>\n";
            xml += "      <map mode='shadow' address='20-3f:8000-ffff'/>\n";
            xml += "      <map mode='linear' address='60-7f:0000-ffff'/>\n";
            xml += "      <map mode='shadow' address='a0-bf:8000-ffff'/>\n";
            xml += "      <map mode='linear' address='e0-ff:0000-ffff'/>\n";
            xml += "    </slot>\n";
            xml += "  </bsx>\n";
        }
        
        xml += "</cartridge>\n";
        xml_memory_map = xml;
    }

    private void read_header(byte[] data, int size)
    {
        //type = Type.Unknown;
        mapper = MemoryMapper.LoROM;
        //dsp1_mapper = DSP1MemoryMapper.DSP1Unmapped;
        region = Region.NTSC;
        rom_size = size;
        ram_size = 0;

        int index = find_header(data, size);
        int mapperid = data[index + HeaderField_Mapper] & 0xFF;
        //byte rom_type = data[index + HeaderField_RomType];
        rom_size = data[index + HeaderField_RomSize];
        //byte company = data[index + HeaderField_Company];
        int regionid = data[index + HeaderField_CartRegion] & 0x7f;

        ram_size = (1024 << (data[index + HeaderField_RamSize] & 7));
        if (ram_size == 1024)
        {
            ram_size = 0;  //no RAM present
        }

        //0, 1, 13 = NTSC; 2 - 12 = PAL
        region = (regionid <= 1 || regionid >= 13) ? Region.NTSC : Region.PAL;

        if (index == 0x7fc0 && size >= 0x401000)
        {
            mapper = MemoryMapper.ExLoROM;
        }
        else if (index == 0x7fc0 && mapperid == 0x32)
        {
            mapper = MemoryMapper.ExLoROM;
        }
        else if (index == 0x7fc0)
        {
            mapper = MemoryMapper.LoROM;
        }
        else if (index == 0xffc0)
        {
            mapper = MemoryMapper.HiROM;
        }
        else
        {  //index == 0x40ffc0
            mapper = MemoryMapper.ExHiROM;
        }
    }

    private int find_header(byte[] data, int size)
    {
        int score_lo = score_header(data, size, 0x007fc0);
        int score_hi = score_header(data, size, 0x00ffc0);
        int score_ex = score_header(data, size, 0x40ffc0);
        if (score_ex!=0)
        {
            score_ex += 4;  //favor ExHiROM on images > 32mbits
        }

        if (score_lo >= score_hi && score_lo >= score_ex)
        {
            return 0x007fc0;
        }
        else if (score_hi >= score_ex)
        {
            return 0x00ffc0;
        }
        else
        {
            return 0x40ffc0;
        }
    }

    private int score_header(byte[] data, int size, int addr)
    {
        if (size < addr + 64)
        {
            return 0;  //image too small to contain header at this location?
        }
        int score = 0;

        int resetvector = (data[addr + HeaderField_ResetVector] & 0xFF) | ((data[addr + HeaderField_ResetVector + 1] & 0xFF) << 8);
        int checksum = (data[addr + HeaderField_Checksum] & 0xFF) | ((data[addr + HeaderField_Checksum + 1] & 0xFF) << 8);
        int complement = (data[addr + HeaderField_Complement] & 0xFF) | ((data[addr + HeaderField_Complement + 1] & 0xFF) << 8);

        int resetop = data[((addr & 0xFFFF) & ~0x7fff) | (resetvector & 0x7fff)] & 0xFF;  //first opcode executed upon reset
        int mapper = (data[addr + HeaderField_Mapper] & ~0x10) & 0xFF;                      //mask off irrelevent FastROM-capable bit

        //$00:[000-7fff] contains uninitialized RAM and MMIO.
        //reset vector must point to ROM at $00:[8000-ffff] to be considered valid.
        if (resetvector < 0x8000)
        {
            return 0;
        }

        //some images duplicate the header in multiple locations, and others have completely
        //invalid header information that cannot be relied upon.
        //below code will analyze the first opcode executed at the specified reset vector to
        //determine the probability that this is the correct header.

        //most likely opcodes
        if (resetop == 0x78  //sei
        || resetop == 0x18  //clc (clc; xce)
        || resetop == 0x38  //sec (sec; xce)
        || resetop == 0x9c  //stz $nnnn (stz $4200)
        || resetop == 0x4c  //jmp $nnnn
        || resetop == 0x5c  //jml $nnnnnn
        )
        {
            score += 8;
        }

        //plausible opcodes
        if (resetop == 0xc2  //rep #$nn
        || resetop == 0xe2  //sep #$nn
        || resetop == 0xad  //lda $nnnn
        || resetop == 0xae  //ldx $nnnn
        || resetop == 0xac  //ldy $nnnn
        || resetop == 0xaf  //lda $nnnnnn
        || resetop == 0xa9  //lda #$nn
        || resetop == 0xa2  //ldx #$nn
        || resetop == 0xa0  //ldy #$nn
        || resetop == 0x20  //jsr $nnnn
        || resetop == 0x22  //jsl $nnnnnn
        )
        {
            score += 4;
        }

        //implausible opcodes
        if (resetop == 0x40  //rti
        || resetop == 0x60  //rts
        || resetop == 0x6b  //rtl
        || resetop == 0xcd  //cmp $nnnn
        || resetop == 0xec  //cpx $nnnn
        || resetop == 0xcc  //cpy $nnnn
        ) score -= 4;

        //least likely opcodes
        if (resetop == 0x00  //brk #$nn
        || resetop == 0x02  //cop #$nn
        || resetop == 0xdb  //stp
        || resetop == 0x42  //wdm
        || resetop == 0xff  //sbc $nnnnnn,x
        )
        {
            score -= 8;
        }

        //at times, both the header and reset vector's first opcode will match ...
        //fallback and rely on info validity in these cases to determine more likely header.

        //a valid checksum is the biggest indicator of a valid header.
        if ((checksum + complement) == 0xffff && (checksum != 0) && (complement != 0))
        {
            score += 4;
        }

        if (addr == 0x007fc0 && mapper == 0x20)
        {
            score += 2;  //0x20 is usually LoROM
        }
        if (addr == 0x00ffc0 && mapper == 0x21)
        {
            score += 2;  //0x21 is usually HiROM
        }
        if (addr == 0x007fc0 && mapper == 0x22)
        {
            score += 2;  //0x22 is usually ExLoROM
        }
        if (addr == 0x40ffc0 && mapper == 0x25)
        {
            score += 2;  //0x25 is usually ExHiROM
        }

        if (data[addr + HeaderField_Company] == 0x33)
        {
            score += 2;        //0x33 indicates extended header
        }
        if (data[addr + HeaderField_RomType] < 0x08)
        {
            score++;
        }
        if (data[addr + HeaderField_RomSize] < 0x10)
        {
            score++;
        }
        if (data[addr + HeaderField_RamSize] < 0x08)
        {
            score++;
        }
        if (data[addr + HeaderField_CartRegion] < 14)
        {
            score++;
        }

        if (score < 0)
        {
            score = 0;
        }
        return score;
    }

    public static final int HeaderField_CartName = 0x00;
    public static final int HeaderField_Mapper = 0x15;
    public static final int HeaderField_RomType = 0x16;
    public static final int HeaderField_RomSize = 0x17;
    public static final int HeaderField_RamSize = 0x18;
    public static final int HeaderField_CartRegion = 0x19;
    public static final int HeaderField_Company = 0x1a;
    public static final int HeaderField_Version = 0x1b;
    public static final int HeaderField_Complement = 0x1c;
    public static final int HeaderField_Checksum = 0x1e;
    public static final int HeaderField_ResetVector = 0x3c;
    //private enum Mode { Normal, BsxSlotted, Bsx, SufamiTurbo, SuperGameBoy }
    //private enum Type { Normal, BsxSlotted, BsxBios, Bsx, SufamiTurboBios, SufamiTurbo, SuperGameBoy1Bios, SuperGameBoy2Bios, GameBoy, Unknown }
    private enum Region { NTSC, PAL }
    private enum MemoryMapper { LoROM, HiROM, ExLoROM, ExHiROM, SuperFXROM, SA1ROM, SPC7110ROM, BSCLoROM, BSCHiROM, BSXROM, STROM }
    //private enum DSP1MemoryMapper { DSP1Unmapped, DSP1LoROM1MB, DSP1LoROM2MB, DSP1HiROM }

    //private bool loaded;        //is a base cartridge inserted?
    //private uint crc32;     //crc32 of all cartridges (base+slot(s))
    private int rom_size;
    private int ram_size;

    //private Mode mode;
    //private Type type;
    private Region region;
    private MemoryMapper mapper;
    //private DSP1MemoryMapper dsp1_mapper;
}
